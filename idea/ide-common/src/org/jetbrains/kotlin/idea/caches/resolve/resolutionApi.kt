/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ResolutionUtils")

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun KtElement.getResolutionFacade(): ResolutionFacade =
    KotlinCacheService.getInstance(project).getResolutionFacade(listOf(this))

/**
 * For local declarations is equivalent to unsafeResolveToDescriptor(bodyResolveMode)
 *
 * But for non-local declarations it ignores bodyResolveMode and uses LazyDeclarationResolver directly
 */
@Deprecated(
    message = "This function has unclear semantics. Please use either unsafeResolveToDescriptor or resolveToDescriptorIfAny instead",
    replaceWith = ReplaceWith("unsafeResolveToDescriptor")
)
fun KtDeclaration.resolveToDescriptor(bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): DeclarationDescriptor =
    getResolutionFacade().resolveToDescriptor(this, bodyResolveMode)

/**
 * This function throws exception when resolveToDescriptorIfAny returns null, otherwise works equivalently.
 *
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun KtDeclaration.unsafeResolveToDescriptor(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): DeclarationDescriptor =
    unsafeResolveToDescriptor(getResolutionFacade(), bodyResolveMode)

/**
 * This function first uses declaration resolvers to resolve this declaration and/or additional declarations (e.g. its parent),
 * and then takes the relevant descriptor from binding context.
 * The exact set of declarations to resolve depends on bodyResolveMode
 *
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun KtDeclaration.resolveToDescriptorIfAny(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): DeclarationDescriptor? =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun KtClassOrObject.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun KtNamedFunction.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun KtProperty.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun KtParameter.resolveToParameterDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToParameterDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun KtElement.resolveToCall(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToCall(getResolutionFacade(), bodyResolveMode)

fun KtFile.resolveImportReference(fqName: FqName): Collection<DeclarationDescriptor> {
    val facade = getResolutionFacade()
    return facade.resolveImportReference(facade.moduleDescriptor, fqName)
}

fun KtAnnotationEntry.resolveToDescriptorIfAny(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): AnnotationDescriptor? =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

// This and next functions are used for 'normal' element analysis
// This analysis *should* provide all information extractable from this KtElement except:
// - for declarations, it does not analyze their bodies
// - for classes, it does not analyze their content
// - for member / top-level properties, it does not analyze initializers / accessors
// This information includes related descriptors, resolved calls (but not inside body, see above!)
// and many other binding context slices.
// Normally, the function is used on local declarations or statements / expressions
// Any usage on non-local declaration is a bit suspicious,
// consider replacing it with resolveToDescriptorIfAny and
// remember that body / content is not analyzed;
// if it's necessary, use analyzeWithContent()
//
// If you need diagnostics in result context, use BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS.
// BodyResolveMode.FULL analyzes all statements on the level of KtElement and above.
// BodyResolveMode.PARTIAL analyzes only statements necessary for this KtElement precise analysis.
//
// See also: ResolveSessionForBodies, ResolveElementCache
/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
@JvmOverloads
fun KtElement.analyze(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext =
    analyze(getResolutionFacade(), bodyResolveMode)

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun KtElement.analyzeAndGetResult(): AnalysisResult {
    return analyzeAndGetResult(getResolutionFacade())
}

fun KtElement.findModuleDescriptor(): ModuleDescriptor = getResolutionFacade().moduleDescriptor

// This function is used on declarations to make analysis not only declaration itself but also it content:
// body for declaration with body, initializer & accessors for properties
/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun KtDeclaration.analyzeWithContent(): BindingContext =
    analyzeWithContent(getResolutionFacade())

// This function is used to make full analysis of declaration container.
// All its declarations, including their content (see above), are analyzed.
/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
inline fun <reified T> T.analyzeWithContent(): BindingContext where T : KtDeclarationContainer, T : KtElement =
    analyzeWithContent(getResolutionFacade())

/**
 * This function is expected to produce the same result as compiler for the whole file content (including diagnostics,
 * trace slices, descriptors, etc.).
 *
 * It's not recommended to call this function without real need.
 *
 * @ref [KotlinCacheService]
 * @ref [org.jetbrains.kotlin.idea.caches.resolve.PerFileAnalysisCache]
 */
fun KtFile.analyzeWithAllCompilerChecks(vararg extraFiles: KtFile): AnalysisResult =
    KotlinCacheService.getInstance(project).getResolutionFacade(listOf(this) + extraFiles.toList())
        .analyzeWithAllCompilerChecks(listOf(this))

/**
 * This function is expected to produce the same result as compiler for the given element and its children (including diagnostics,
 * trace slices, descriptors, etc.). For some expression element it actually performs analyze for some parent (usually declaration).
 *
 * It's not recommended to call this function without real need.
 *
 * NB: for statements / expressions, usually should be replaced with analyze(),
 * for declarations, analyzeWithContent() will do what you want.
 *
 * @ref [KotlinCacheService]
 * @ref [org.jetbrains.kotlin.idea.caches.resolve.PerFileAnalysisCache]
 */
@Deprecated(
    "Use either KtFile.analyzeWithAllCompilerChecks() or KtElement.analyzeAndGetResult()",
    ReplaceWith("analyzeAndGetResult()")
)
fun KtElement.analyzeWithAllCompilerChecks(): AnalysisResult = getResolutionFacade().analyzeWithAllCompilerChecks(listOf(this))

// this method don't check visibility and collect all descriptors with given fqName
fun ResolutionFacade.resolveImportReference(
    moduleDescriptor: ModuleDescriptor,
    fqName: FqName
): Collection<DeclarationDescriptor> {
    val importDirective = KtPsiFactory(project).createImportDirective(ImportPath(fqName, false))
    val qualifiedExpressionResolver = this.getFrontendService(moduleDescriptor, QualifiedExpressionResolver::class.java)
    return qualifiedExpressionResolver.processImportReference(
        importDirective,
        moduleDescriptor,
        BindingTraceContext(),
        excludedImportNames = emptyList(),
        packageFragmentForVisibilityCheck = null
    )?.getContributedDescriptors() ?: emptyList()
}

@Suppress("DEPRECATION")
@Deprecated(
    "This method is going to be removed in 1.3.0 release",
    ReplaceWith("analyzeWithAllCompilerChecks().bindingContext"),
    DeprecationLevel.ERROR
)
fun KtElement.analyzeFully(): BindingContext = analyzeWithAllCompilerChecks().bindingContext