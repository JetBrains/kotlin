/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("ResolutionUtils")

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException

fun KtElement.getResolutionFacade(): ResolutionFacade =
        KotlinCacheService.getInstance(project).getResolutionFacade(listOf(this))

/**
 * For local declarations is equivalent to unsafeResolveToDescriptor(bodyResolveMode)
 *
 * But for non-local declarations it ignores bodyResolveMode and uses LazyDeclarationResolver directly
 */
@Deprecated(message = "This function has unclear semantics. Please use either unsafeResolveToDescriptor or resolveToDescriptorIfAny instead",
            replaceWith = ReplaceWith("unsafeResolveToDescriptor"))
fun KtDeclaration.resolveToDescriptor(bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): DeclarationDescriptor =
        getResolutionFacade().resolveToDescriptor(this, bodyResolveMode)

/**
 * This function throws exception when resolveToDescriptorIfAny returns null, otherwise works equivalently.
 */
fun KtDeclaration.unsafeResolveToDescriptor(bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): DeclarationDescriptor =
        resolveToDescriptorIfAny(bodyResolveMode) ?: throw NoDescriptorForDeclarationException(this)

/**
 * This function first uses declaration resolvers to resolve this declaration and/or additional declarations (e.g. its parent),
 * and then takes the relevant descriptor from binding context.
 * The exact set of declarations to resolve depends on bodyResolveMode
 */
fun KtDeclaration.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL): DeclarationDescriptor? {
    //TODO: BodyResolveMode.PARTIAL is not quite safe!
    val context = analyze(bodyResolveMode)
    return if (this is KtParameter && hasValOrVar()) {
        context.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, this)
    } else {
        context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
    }
}

fun KtClassOrObject.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL): ClassDescriptor? {
    return (this as KtDeclaration).resolveToDescriptorIfAny(bodyResolveMode) as? ClassDescriptor
}

fun KtNamedFunction.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL): FunctionDescriptor? {
    return (this as KtDeclaration).resolveToDescriptorIfAny(bodyResolveMode) as? FunctionDescriptor
}

fun KtProperty.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL): VariableDescriptor? {
    return (this as KtDeclaration).resolveToDescriptorIfAny(bodyResolveMode) as? VariableDescriptor
}

fun KtParameter.resolveToParameterDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL): ValueParameterDescriptor? {
    val context = analyze(bodyResolveMode)
    return context.get(BindingContext.VALUE_PARAMETER, this) as? ValueParameterDescriptor
}

fun KtElement.resolveToCall(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL): ResolvedCall<out CallableDescriptor>? =
    getResolvedCall(analyze(bodyResolveMode))

fun KtFile.resolveImportReference(fqName: FqName): Collection<DeclarationDescriptor> {
    val facade = getResolutionFacade()
    return facade.resolveImportReference(facade.moduleDescriptor, fqName)
}


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
@JvmOverloads
fun KtElement.analyze(bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext =
    getResolutionFacade().analyze(this, bodyResolveMode)

fun KtElement.analyzeAndGetResult(): AnalysisResult {
    val resolutionFacade = getResolutionFacade()
    return AnalysisResult.success(resolutionFacade.analyze(this), resolutionFacade.moduleDescriptor)
}

fun KtElement.findModuleDescriptor(): ModuleDescriptor = getResolutionFacade().moduleDescriptor

// This function is used on declarations to make analysis not only declaration itself but also it content:
// body for declaration with body, initializer & accessors for properties
fun KtDeclaration.analyzeWithContent(): BindingContext =
    getResolutionFacade().analyzeWithAllCompilerChecks(listOf(this)).bindingContext

// This function is used to make full analysis of declaration container.
// All its declarations, including their content (see above), are analyzed.
inline fun <reified T> T.analyzeWithContent(): BindingContext where T : KtDeclarationContainer, T : KtElement =
    getResolutionFacade().analyzeWithAllCompilerChecks(listOf(this)).bindingContext

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
    KotlinCacheService.getInstance(project).getResolutionFacade(listOf(this) + extraFiles.toList()).analyzeWithAllCompilerChecks(listOf(this))

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
            importDirective, moduleDescriptor, BindingTraceContext(), excludedImportNames = emptyList(), packageFragmentForVisibilityCheck = null)?.getContributedDescriptors() ?: emptyList()
}

@Suppress("DEPRECATION")
@Deprecated(
    "This method is going to be removed in 1.3.0 release",
    ReplaceWith("analyzeWithAllCompilerChecks().bindingContext"),
    DeprecationLevel.ERROR
)
fun KtElement.analyzeFully(): BindingContext = analyzeWithAllCompilerChecks().bindingContext