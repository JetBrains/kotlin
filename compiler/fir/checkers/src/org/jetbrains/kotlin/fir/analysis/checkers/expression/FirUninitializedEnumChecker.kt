/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature.ProperUninitializedEnumEntryAccessAnalysis
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isNonLocal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.utils.addToStdlib.flatAssociateBy

object FirUninitializedEnumChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    // Initialization order: member property initializers, enum entries, companion object (including members in it).
    //
    // When JVM loads a class, the corresponding class initializer, a.k.a. <clinit>, is executed first.
    // Kotlin (and Java as well) converts enum entries as static final field, which is initialized in that <clinit>:
    //   enum class E(...) {
    //     E1, E2, ...
    //   }
    //     ~>
    //   class E {
    //     final static E1, E2, ...
    //     static { // <clinit>
    //       E1 = new E(...)
    //       ...
    //     }
    //   }
    //
    // Note that, when initializing enum entries, now we call the enum class's constructor, a.k.a. <init>, to initialize non-final
    // instance members. Therefore, if there is a member property in the enum class, and if that member has access to enum entries,
    // that is illegal access since enum entries are not yet initialized:
    //   enum class E(...) {
    //     E1, E2, ...
    //     val m1 = E1
    //   }
    //     ~>
    //   class E {
    //     E m1 ...
    //     E(...) { // <init>
    //       m1 = E1
    //     }
    //     final static E1, E2, ...
    //     static { // <clinit>
    //       E1 = new E(...)
    //       ...
    //     }
    //   }
    //
    // A companion object is desugared to a static final singleton, and initialized in <clinit> too. However, enum lowering goes first,
    // or in other words, companion object lowering goes last. Thus, any other things initialized in <clinit>, including enum entries,
    // should not have access to companion object and members in it.
    //
    // See related discussions:
    // https://youtrack.jetbrains.com/issue/KT-6054
    // https://youtrack.jetbrains.com/issue/KT-11769
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // If the feature for proper analysis is enabled, FirEnumEntryInitializationChecker will report all errors
        if (context.languageVersionSettings.supportsFeature(ProperUninitializedEnumEntryAccessAnalysis)) return
        val source = expression.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        val calleeSymbol = expression.calleeReference.toResolvedBaseSymbol() ?: return
        val enumClassSymbol = calleeSymbol.getContainingClassSymbol() as? FirRegularClassSymbol ?: return
        // We're looking for members/entries/companion object in an enum class or members in companion object of an enum class.
        if (!enumClassSymbol.isEnumClass) return

        // Local enum class are prohibited
        // So report error on access of local enum entry
        if (enumClassSymbol.visibility == Visibilities.Local && calleeSymbol is FirEnumEntrySymbol) {
            reporter.reportOn(source, FirErrors.UNINITIALIZED_ENUM_ENTRY, calleeSymbol, context)
        }

        // An accessed context within the enum class of interest. We should look up until either enum members or enum entries are found,
        // not just last containing declaration. For example,
        //   enum class Fruit(...) {
        //     APPLE(...);
        //     companion object {
        //       val common = ...
        //     }
        //     val score = ... <!>common<!>
        //     val score2 = { ... <!>common<!> }()
        //   }
        // companion object is not initialized for both member properties. The former has the property itself as the last containing
        // declaration, whereas the latter has an anonymous function instead. For both cases, we're looking for member properties as an
        // accessed context.
        val isInsideCorrespondingEnum = context.containingDeclarations.any {
            it.getContainingClassSymbol() == enumClassSymbol
        }
        if (!isInsideCorrespondingEnum) return

        @OptIn(DirectDeclarationsAccess::class)
        val declarationSymbols = enumClassSymbol.declarationSymbols
        val enumMemberProperties = declarationSymbols.filterIsInstance<FirPropertySymbol>()
        val enumInitBlocks = declarationSymbols.filterIsInstance<FirAnonymousInitializerSymbol>()
        val enumEntries = declarationSymbols.filterIsInstance<FirEnumEntrySymbol>()
        val enumEntriesInitBlocks = enumEntries.flatAssociateBy {
            @OptIn(DirectDeclarationsAccess::class)
            it.initializerObjectSymbol?.declarationSymbols?.filterIsInstance<FirAnonymousInitializerSymbol>().orEmpty()
        }

        val accessedContext = context.containingDeclarations.lastOrNull {
            // To not raise an error for access from another enum class, e.g.,
            //   enum class EnumCompanion4(...) {
            //     INSTANCE(EnumCompanion2.foo())
            //   }
            // find an accessed context within the same enum class.
            it.getContainingClassSymbol() == enumClassSymbol || it.symbol in enumEntriesInitBlocks
        }?.symbol ?: return

        // When checking enum member properties, accesses to enum entries in lazy delegation is legitimate, e.g.,
        //   enum JvmTarget(...) {
        //     JVM_1_6, ...
        //     val bytecodeVersion: ... by lazy {
        //       when (this) {
        //           JVM_1_6 -> ...
        //     }
        //   }
        val containingDeclarationForAccess = context.containingDeclarations.lastOrNull {
            when (it) {
                // for members of local classes `isNonLocal` returns `false`
                is FirCallableDeclaration -> it.isNonLocal || it.dispatchReceiverType != null
                else -> false
            }
        }

        if (accessedContext in enumMemberProperties) {
            val lazyDelegation = (accessedContext as FirPropertySymbol).lazyDelegation
            if (lazyDelegation != null) {
                return
            }
        }

        // When checking enum entries, only entry initializer matters. For example,
        //   enum class EnumCompanion(...) {
        //     ANOTHER {
        //       override fun bar() = foo() // At this point, companion object is initialized
        //     }
        //     abstract bar(): ...
        //     companion object {
        //       fun foo() = ...
        //     }
        //   }
        if (accessedContext in enumEntries && containingDeclarationForAccess?.isEnumEntryInitializer() != true) {
            return
        }

        // The enum entries of an enum class
        if (calleeSymbol in enumEntries) {
            val calleeEnumEntry = calleeSymbol as FirEnumEntrySymbol
            // Uninitialized from the point of view of members of that enum class

            fun reportIllegalAccessInEnumEntry(correspondingEnumEntry: FirEnumEntrySymbol) {
                // Technically, this is equal to `enumEntries.indexOf(accessedContext) <= enumEntries.indexOf(calleeDeclaration)`.
                // Instead of double `indexOf`, we can iterate entries just once until either one appears.
                var precedingEntry: FirEnumEntrySymbol? = null
                for (it in enumEntries) {
                    if (it == calleeEnumEntry || it == correspondingEnumEntry) {
                        precedingEntry = it
                        break
                    }
                }
                if (precedingEntry == correspondingEnumEntry) {
                    reporter.reportOn(source, FirErrors.UNINITIALIZED_ENUM_ENTRY, calleeEnumEntry, context)
                }
            }

            when (accessedContext) {
                in enumMemberProperties,
                in enumInitBlocks -> {
                    /* From KT-6054
                     * enum class MyEnum {
                     *   A, B;
                     *   val x = when(this) {
                     *     <!>A<!> -> ...
                     *     <!>B<!> -> ...
                     *   }
                     * }
                     */
                    reporter.reportOn(source, FirErrors.UNINITIALIZED_ENUM_ENTRY, calleeEnumEntry, context)
                }

                in enumEntries -> {
                    /*
                     * enum class A(...) {
                     *   A1(<!>A2<!>),
                     *   A2(...),
                     *   A3(<!>A3<!>)
                     * }
                     */
                    reportIllegalAccessInEnumEntry(accessedContext as FirEnumEntrySymbol)
                }

                in enumEntriesInitBlocks -> {
                    /*
                     * enum class MyEnum {
                     *   A {
                     *     init { A }
                     *   },
                     *   B;
                     * }
                     */
                    val entrySymbol = enumEntriesInitBlocks.getValue(accessedContext as FirAnonymousInitializerSymbol)
                    /*
                     * In init block of entry this entry already initialized, so it's safe to access it
                     * enum class MyEnum {
                     *   A {
                     *     init {
                     *       A // safe
                     *       B // unsafe
                     *     }
                     *   },
                     *   B
                     * }
                     */
                    if (entrySymbol == calleeSymbol) return
                    reportIllegalAccessInEnumEntry(entrySymbol)
                }
            }
        }
    }

    private val FirPropertySymbol.lazyDelegation: FirAnonymousFunction?
        get() {
            lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            @OptIn(SymbolInternals::class)
            val property = this.fir
            if (property.delegate == null || property.delegate !is FirFunctionCall) return null
            val delegateCall = property.delegate as FirFunctionCall
            val calleeSymbol = delegateCall.calleeReference.toResolvedNamedFunctionSymbol() ?: return null
            if (calleeSymbol.callableId.asSingleFqName().asString() != "kotlin.lazy") return null
            val lazyCallArgument = delegateCall.argumentList.arguments.singleOrNull() as? FirAnonymousFunctionExpression ?: return null
            return lazyCallArgument.anonymousFunction
        }

    private fun FirDeclaration.isEnumEntryInitializer(): Boolean {
        val containingClassSymbol = when (this) {
            is FirConstructor -> {
                if (!isPrimary) return false
                (containingClassForStaticMemberAttr as? ConeClassLikeLookupTagWithFixedSymbol)?.symbol
            }
            is FirAnonymousInitializer -> {
                containingDeclarationSymbol as? FirClassSymbol
            }
            else -> null
        } ?: return false
        return containingClassSymbol.classKind == ClassKind.ENUM_ENTRY
    }
}
