/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isEnumEntryInitializer
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClass
import org.jetbrains.kotlin.fir.analysis.checkers.outerClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference

object FirUninitializedEnumChecker : FirQualifiedAccessExpressionChecker() {
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
    // instance members. Therefore, if there is a member property in the enum class, and if that member has an access to enum entries,
    // that is an illegal access since enum entries are not yet initialized:
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
        val source = expression.source ?: return
        if (source.kind is FirFakeSourceElementKind) return

        val reference = expression.calleeReference as? FirResolvedNamedReference ?: return
        val calleeDeclaration = reference.resolvedSymbol.fir
        val calleeContainingClass = calleeDeclaration.getContainingClass(context) as? FirRegularClass ?: return
        // We're looking for members/entries/companion object in an enum class or members in companion object of an enum class.
        val calleeIsInsideEnum = calleeContainingClass.isEnumClass
        val calleeIsInsideEnumCompanion =
            calleeContainingClass.isCompanion && (calleeContainingClass.outerClass(context) as? FirRegularClass)?.isEnumClass == true
        if (!calleeIsInsideEnum && !calleeIsInsideEnumCompanion) return

        val enumClass =
            if (calleeIsInsideEnum) calleeContainingClass
            else calleeContainingClass.outerClass(context) as? FirRegularClass ?: return

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
        val accessedContext = context.containingDeclarations.lastOrNull {
            // To not raise an error for an access from another enum class, e.g.,
            //   enum class EnumCompanion4(...) {
            //     INSTANCE(EnumCompanion2.foo())
            //   }
            // find an accessed context within the same enum class.
            (it as? FirSymbolOwner<*>)?.getContainingClass(context) == enumClass
        } ?: return

        val enumMemberProperties = enumClass.declarations.filterIsInstance<FirProperty>()
        val enumEntries = enumClass.declarations.filterIsInstance<FirEnumEntry>()

        // When checking enum member properties, accesses to enum entries in lazy delegation is legitimate, e.g.,
        //   enum JvmTarget(...) {
        //     JVM_1_6, ...
        //     val bytecodeVersion: ... by lazy {
        //       when (this) {
        //           JVM_1_6 -> ...
        //     }
        //   }
        if (accessedContext in enumMemberProperties) {
            val lazyDelegation = (accessedContext as FirProperty).lazyDelegation
            if (lazyDelegation != null && lazyDelegation == context.containingDeclarations.lastOrNull()) {
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
        if (accessedContext in enumEntries && context.containingDeclarations.lastOrNull()?.isEnumEntryInitializer != true) {
            return
        }

        // Members inside the companion object of an enum class
        if (calleeContainingClass == enumClass.companionObject) {
            // Uninitialized from the point of view of members or enum entries of that enum class
            if (accessedContext in enumMemberProperties || accessedContext in enumEntries) {
                if (calleeDeclaration is FirProperty) {
                    // From KT-11769
                    // enum class Fruit(...) {
                    //   APPLE(...);
                    //   companion object {
                    //     val common = ...
                    //   }
                    //   val score = ... <!>common<!>
                    // }
                    reporter.reportOn(source, FirErrors.UNINITIALIZED_VARIABLE, calleeDeclaration.symbol, context)
                } else {
                    // enum class EnumCompanion2(...) {
                    //   INSTANCE(<!>foo<!>())
                    //   companion object {
                    //     fun foo() = ...
                    //   }
                    // }
                    // <!>Companion<!>.foo() v.s. <!>foo<!>()
                    if ((expression.explicitReceiver as? FirResolvedQualifier)?.symbol?.fir == enumClass.companionObject) {
                        reporter.reportOn(
                            expression.explicitReceiver!!.source,
                            FirErrors.UNINITIALIZED_ENUM_COMPANION,
                            enumClass.symbol,
                            context
                        )
                    } else {
                        reporter.reportOn(
                            expression.calleeReference.source,
                            FirErrors.UNINITIALIZED_ENUM_COMPANION,
                            enumClass.symbol,
                            context
                        )
                    }
                }
            }
        }

        // The enum entries of an enum class
        if (calleeDeclaration in enumEntries) {
            val calleeEnumEntry = calleeDeclaration as FirEnumEntry
            // Uninitialized from the point of view of members of that enum class
            if (accessedContext in enumMemberProperties) {
                // From KT-6054
                // enum class MyEnum {
                //   A, B;
                //   val x = when(this) {
                //     <!>A<!> -> ...
                //     <!>B<!> -> ...
                //   }
                // }
                reporter.reportOn(source, FirErrors.UNINITIALIZED_ENUM_ENTRY, calleeEnumEntry.symbol, context)
            }
            // enum class A(...) {
            //   A1(<!>A2<!>),
            //   A2(...),
            //   A3(<!>A3<!>)
            // }
            if (accessedContext in enumEntries) {
                // Technically, this is equal to `enumEntries.indexOf(accessedContext) <= enumEntries.indexOf(calleeDeclaration)`.
                // Instead of double `indexOf`, we can iterate entries just once until either one appears.
                var precedingEntry: FirEnumEntry? = null
                enumEntries.forEach {
                    if (precedingEntry != null) return@forEach
                    if (it == calleeEnumEntry || it == accessedContext) {
                        precedingEntry = it
                    }
                }
                if (precedingEntry == accessedContext) {
                    reporter.reportOn(source, FirErrors.UNINITIALIZED_ENUM_ENTRY, calleeEnumEntry.symbol, context)
                }
            }
        }
    }

    private val FirProperty.lazyDelegation: FirAnonymousFunction?
        get() {
            if (delegate == null || delegate !is FirFunctionCall) return null
            val delegateCall = delegate as FirFunctionCall
            val callee =
                (delegateCall.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirSimpleFunction ?: return null
            if (callee.symbol.callableId.asSingleFqName().asString() != "kotlin.lazy") return null
            val lazyCallArgument = delegateCall.argumentList.arguments.singleOrNull() as? FirLambdaArgumentExpression ?: return null
            return (lazyCallArgument.expression as? FirAnonymousFunctionExpression)?.anonymousFunction
        }
}
