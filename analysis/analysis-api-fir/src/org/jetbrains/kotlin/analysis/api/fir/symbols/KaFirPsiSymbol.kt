/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.codeInsight.PsiEquivalenceUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePsiSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.supertypeGenerators
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * A [KaFirSymbol] that is possibly backed by some [PsiElement] and builds [firSymbol] lazily (by convention),
 * allowing some properties to be calculated without the need to build a [firSymbol].
 */
internal interface KaFirPsiSymbol<out P : PsiElement, out S : FirBasedSymbol<*>> : KaFirSymbol<S> {
    /**
     * The [PsiElement] which can be used as a source of truth for some other property implementations.
     *
     * It can be as an element from a source file, or an element from a library.
     */
    val backingPsi: P?

    /**
     * The lazy implementation of [FirBasedSymbol].
     *
     * The implementation is either built on top of [backingPsi] or provided during creation.
     *
     * @see firSymbol
     */
    val lazyFirSymbol: Lazy<S>

    /**
     * The origin should be provided without using [firSymbol], if possible.
     */
    abstract override val origin: KaSymbolOrigin

    override val firSymbol: S get() = lazyFirSymbol.value
}

internal val FirBasedSymbol<*>.backingPsiIfApplicable: PsiElement?
    get() {
        if (origin == FirDeclarationOrigin.Synthetic.TypeAliasConstructor) return null

        return fir.realPsi
    }

internal interface KaFirKtBasedSymbol<out P : KtElement, out S : FirBasedSymbol<*>> : KaFirPsiSymbol<P, S> {
    override val origin: KaSymbolOrigin get() = withValidityAssertion { psiOrSymbolOrigin() }
}

internal fun KaFirPsiSymbol<*, *>.psiOrSymbolHashCode(): Int = backingPsi?.hashCode() ?: symbolHashCode()
internal fun KaFirPsiSymbol<*, *>.psiOrSymbolEquals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || other::class != this::class) return false

    val backingPsi = backingPsi
    val otherBackingPsi = (other as KaFirPsiSymbol<*, *>).backingPsi
    when {
        // Both elements are null, so we cannot compare them
        backingPsi == null && otherBackingPsi == null -> return firSymbol == other.firSymbol

        // Special handling for Java declarations as we cannot guarantee their identity
        backingPsi is PsiMember || otherBackingPsi is PsiMember -> {
            return backingPsi != null &&
                    otherBackingPsi != null &&
                    PsiEquivalenceUtil.areElementsEquivalent(backingPsi, otherBackingPsi)
        }

        backingPsi !== otherBackingPsi -> return false
    }

    if (backingPsi !is KtElement) {
        errorWithAttachment("Unexpected backingPsi class: ${backingPsi?.let { it::class.simpleName }}") {
            withPsiEntry("backingPsi", backingPsi)
            withSymbolAttachment("this", analysisSession, this@psiOrSymbolEquals)
        }
    }

    // Source PSI elements represents only source symbols
    if (!backingPsi.cameFromKotlinLibrary) return true

    // As library elements may represent both library and source symbols at once, we have to check the FIR symbol equals
    return firSymbol == other.firSymbol
}

/**
 * Note: This function is supposed only for simple cases there annotations can be declared only
 * directly on the underlying [KtAnnotated], so cases like property accessors or
 * generated constructor property are not supported.
 */
internal fun KaFirKtBasedSymbol<KtAnnotated, *>.psiOrSymbolAnnotationList(): KaAnnotationList {
    if (backingPsi?.annotationEntries?.isEmpty() == true) {
        return KaBaseEmptyAnnotationList(token)
    }

    return KaFirAnnotationListForDeclaration.create(firSymbol, builder)
}

internal fun KaFirKtBasedSymbol<KtCallableDeclaration, FirCallableSymbol<*>>.createContextReceivers(): List<KaContextReceiver> {
    val psi = backingPsi
    if (psi != null && (psi !is KtTypeParameterListOwnerStub<*> || psi.contextReceiverList == null)) return emptyList()

    return firSymbol.createContextReceivers(builder)
}

/**
 * We cannot optimize super types by psi if at least one compiler plugin may generate additional types
 */
private fun KaFirSession.hasCompilerPluginForSupertypes(declaration: KtClassOrObject): Boolean {
    val declarationSiteModule = getModule(declaration)
    val declarationSiteSession = resolutionFacade.getSessionFor(declarationSiteModule)
    return declarationSiteSession.extensionService.supertypeGenerators.isNotEmpty()
}

/**
 * We cannot optimize some declaration creations if at least one compiler plugin may generate additional declarations
 */
internal fun KaFirSession.hasDeclarationGeneratorCompilerPlugin(declaration: KtClassOrObject): Boolean {
    val declarationSiteModule = getModule(declaration)
    val declarationSiteSession = resolutionFacade.getSessionFor(declarationSiteModule)
    return declarationSiteSession.extensionService.declarationGenerators.isNotEmpty()
}

internal fun KaFirKtBasedSymbol<KtClassOrObject, FirClassSymbol<*>>.createSuperTypes(): List<KaType> {
    /**
     * There is no so much profit to analyze PSI from libraries, but it requires additional logic
     * as we may have additional providers like [org.jetbrains.kotlin.fir.deserialization.FirDeserializationExtension]
     * or [org.jetbrains.kotlin.fir.deserialization.addCloneForArrayIfNeeded].
     */
    val backingPsi = ifSource { backingPsi }

    if (backingPsi?.superTypeListEntries?.isNotEmpty() != false || analysisSession.hasCompilerPluginForSupertypes(backingPsi)) {
        return firSymbol.superTypesList(builder)
    }

    val specialSuperType = when {
        backingPsi !is KtClass || this !is KaNamedClassSymbol -> null
        backingPsi.isAnnotation() -> analysisSession.builtinTypes.annotationType
        backingPsi.isEnum() -> with(analysisSession) {
            val enumFirSymbol = firSession.builtinTypes.enumType.toRegularClassSymbol(firSession)
                ?: return firSymbol.superTypesList(builder) // something goes wrong here

            val enumKaSymbol = builder.classifierBuilder.buildNamedClassSymbol(enumFirSymbol)
            buildClassType(enumKaSymbol) {
                argument(defaultType)
            }
        }

        backingPsi.isData() && JvmStandardClassIds.Annotations.JvmRecord in annotations -> {
            val jvmRecordSymbol = analysisSession.findClass(JvmStandardClassIds.Java.Record) as? KaNamedClassSymbol
            with(analysisSession) {
                jvmRecordSymbol?.defaultType
            }
        }

        else -> when (classId) {
            StandardClassIds.Any, StandardClassIds.Nothing -> return emptyList()
            else -> null
        }
    }

    return listOf(specialSuperType ?: analysisSession.builtinTypes.any)
}

/**
 * Currently, the compiled file can represent both library and non-library origin depending on the `preferBinary`
 * parameter from [org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache.getSession].
 *
 * So, depending on it, we may represent one decompiled file as [KaSymbolOrigin.SOURCE] and as [KaSymbolOrigin.LIBRARY]
 * at the same time.
 */
internal fun <P : KtElement> KaFirPsiSymbol<P, *>.psiOrSymbolOrigin(): KaSymbolOrigin {
    val backingPsi = backingPsi
    return when {
        backingPsi == null -> symbolOrigin()
        backingPsi.cameFromKotlinLibrary -> symbolOrigin()
        else -> KaSymbolOrigin.SOURCE
    }
}

private val KtElement.cameFromKotlinLibrary: Boolean get() = containingKtFile.isCompiled

/**
 * Executes [action] if the [KaFirPsiSymbol.backingPsi] exists and came from a source file.
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <R> KaFirPsiSymbol<*, *>.ifSource(action: () -> R): R? {
    contract {
        callsInPlace(action, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    return if ((backingPsi as? KtElement)?.cameFromKotlinLibrary != false) null else action()
}

/**
 * Potentially, we may use [KaFirKtBasedSymbol.backingPsi] to create [org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePsiSymbolPointer] for library elements as well,
 * but it triggers AST tree calculation.
 *
 * Another potential issue: the library PSI may represent both [KaSymbolOrigin.SOURCE] and as [KaSymbolOrigin.LIBRARY],
 * so it is not so simple to distinguish between them to restore the correct symbol.
 */
internal inline fun <reified S : KaSymbol> KaFirKtBasedSymbol<*, *>.psiBasedSymbolPointerOfTypeIfSource(): KaSymbolPointer<S>? {
    return ifSource {
        backingPsi?.let {
            KaBasePsiSymbolPointer(it, S::class, this as S)
        }
    }
}

internal inline fun <reified S : FirBasedSymbol<*>> lazyFirSymbol(
    declaration: KtDeclaration,
    session: KaFirSession,
): Lazy<S> = lazyPub {
    declaration.resolveToFirSymbolOfType<S>(session.resolutionFacade)
}

/**
 * This function is a workaround for the KT-70728 issue.
 *
 * The problem is that library sources share the underlying PSI with binary modules, and
 * the use site session is not enough to build the correct FIR from PSI.
 * Hence, we cannot, for instance, use [createKaValueParameters] from a library source PSI as
 * it may create a FIR from unrelated module, and we will have an inconsistency.
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <R> KaFirPsiSymbol<*, *>.ifNotLibrarySource(action: () -> R): R? {
    contract {
        callsInPlace(action, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    return if (analysisSession.useSiteModule is KaLibrarySourceModule) null else action()
}

internal fun KaFirKtBasedSymbol<KtCallableDeclaration, *>.createKaValueParameters(): List<KaValueParameterSymbol>? =
    ifNotLibrarySource {
        with(analysisSession) {
            backingPsi?.valueParameters?.map { it.symbol as KaValueParameterSymbol }
        }
    }

internal fun KaFirKtBasedSymbol<KtCallableDeclaration, *>.createKaContextParameters(): List<KaContextParameterSymbol>? =
    ifNotLibrarySource {
        val psi = backingPsi as? KtTypeParameterListOwnerStub<*> ?: return null // no psi
        val lists = psi.contextReceiverLists.ifEmpty { return emptyList() } // no context receivers/parameters
        with(analysisSession) {
            lists.flatMap { list ->
                val contextParameters = list.contextParameters()
                if (contextParameters.isNotEmpty()) {
                    contextParameters.map { it.symbol as KaContextParameterSymbol }
                } else {
                    list.contextReceivers().map { it.symbol }
                }
            }
        }
    }

internal fun KaFirKtBasedSymbol<KtTypeParameterListOwner, *>.createKaTypeParameters(): List<KaTypeParameterSymbol>? =
    ifNotLibrarySource {
        with(analysisSession) {
            backingPsi?.typeParameters?.map { it.symbol }
        }
    }

internal fun KaFirKtBasedSymbol<KtDeclarationWithBody, FirCallableSymbol<*>>.createReturnType(): KaType {
    val backingPsi = backingPsi
    if (backingPsi?.hasBlockBody() == true && !backingPsi.hasDeclaredReturnType()) {
        return analysisSession.builtinTypes.unit
    }

    return firSymbol.returnType(builder)
}
