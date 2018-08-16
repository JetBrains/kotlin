/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

private class ResolutionFacadeWithDebugInfo(
    private val delegate: ResolutionFacade,
    private val creationPlace: CreationPlace
) : ResolutionFacade {
    override val project: Project
        get() = delegate.project

    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext {
        return wrapExceptions({ ResolvingWhat(listOf(element), bodyResolveMode) }) {
            delegate.analyze(element, bodyResolveMode)
        }
    }

    override fun analyze(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        return wrapExceptions({ ResolvingWhat(elements, bodyResolveMode) }) {
            delegate.analyze(elements, bodyResolveMode)
        }
    }

    override fun analyzeWithAllCompilerChecks(elements: Collection<KtElement>): AnalysisResult {
        return wrapExceptions({ ResolvingWhat(elements) }) {
            delegate.analyzeWithAllCompilerChecks(elements)
        }
    }

    override fun resolveToDescriptor(declaration: KtDeclaration, bodyResolveMode: BodyResolveMode): DeclarationDescriptor {
        return wrapExceptions({ ResolvingWhat(listOf(declaration), bodyResolveMode) }) {
            delegate.resolveToDescriptor(declaration, bodyResolveMode)
        }
    }

    override val moduleDescriptor: ModuleDescriptor
        get() = delegate.moduleDescriptor

    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
        return wrapExceptions({ ResolvingWhat(serviceClass = serviceClass) }) {
            delegate.getFrontendService(serviceClass)
        }
    }

    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        return wrapExceptions({ ResolvingWhat(serviceClass = serviceClass) }) {
            delegate.getIdeService(serviceClass)
        }
    }

    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        return wrapExceptions({ ResolvingWhat(listOf(element), serviceClass = serviceClass) }) {
            delegate.getFrontendService(element, serviceClass)
        }
    }

    override fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T? {
        return wrapExceptions({ ResolvingWhat(listOf(element), serviceClass = serviceClass) }) {
            delegate.tryGetFrontendService(element, serviceClass)
        }
    }

    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        return wrapExceptions({ ResolvingWhat(serviceClass = serviceClass, moduleDescriptor = moduleDescriptor) }) {
            delegate.getFrontendService(moduleDescriptor, serviceClass)
        }
    }

    private inline fun <R> wrapExceptions(resolvingWhat: () -> ResolvingWhat, body: () -> R): R {
        try {
            return body()
        } catch (e: Throwable) {
            if (e is ControlFlowException) {
                throw e
            }
            throw KotlinIdeaResolutionException(e, resolvingWhat(), creationPlace)
        }
    }
}

private class KotlinIdeaResolutionException(
    cause: Throwable,
    resolvingWhat: ResolvingWhat,
    creationPlace: CreationPlace
) : KotlinExceptionWithAttachments("Kotlin resolution encountered a problem while ${resolvingWhat.shortDescription()}", cause) {
    init {
        withAttachment("info.txt", buildString {
            append(resolvingWhat.description())
            appendln("---------------------------------------------")
            append(creationPlace.description())
        })
    }
}


private class CreationPlace(
    private val elements: Collection<KtElement>,
    private val moduleInfo: ModuleInfo?,
    private val platform: TargetPlatform?
) {
    fun description() = buildString {
        appendln("Resolver created for:")
        for (element in elements) {
            appendElement(element)
        }
        if (moduleInfo != null) {
            appendln("Provided module info: $moduleInfo")
        }
        if (platform != null) {
            appendln("Provided platform: $platform")
        }
    }
}

private class ResolvingWhat(
    private val elements: Collection<PsiElement> = emptyList(),
    private val bodyResolveMode: BodyResolveMode? = null,
    private val serviceClass: Class<*>? = null,
    private val moduleDescriptor: ModuleDescriptor? = null
) {
    fun shortDescription() = serviceClass?.let { "getting service ${serviceClass.simpleName}" }
        ?: "analyzing ${elements.firstOrNull()?.javaClass?.simpleName ?: ""}"

    fun description(): String {
        return buildString {
            appendln("Failed performing task:")
            if (serviceClass != null) {
                appendln("Getting service: ${serviceClass.name}")
            } else {
                append("Analyzing code")
                if (bodyResolveMode != null) {
                    append(" in BodyResolveMode.$bodyResolveMode")
                }
                appendln()
            }
            appendln("Elements:")
            for (element in elements) {
                appendElement(element)
            }
            if (moduleDescriptor != null) {
                appendln("Provided module descriptor for module ${moduleDescriptor.getCapability(ModuleInfo.Capability)}")
            }
        }
    }
}

private fun StringBuilder.appendElement(element: PsiElement) {
    fun info(key: String, value: String?) {
        appendln("  $key = $value")
    }

    appendln("Element of type: ${element.javaClass.simpleName}:")
    if (element is PsiNamedElement) {
        info("name", element.name)
    }
    info("isValid", element.isValid.toString())
    info("isPhysical", element.isPhysical.toString())
    if (element is PsiFile) {
        info("containingFile.name", element.containingFile.name)
    }
    val moduleInfo = ifIndexReady { element.getModuleInfo() }
    info("moduleInfo", moduleInfo?.toString() ?: "<index not ready>")
    if (moduleInfo != null) {
        info("moduleInfo.platform", moduleInfo.platform?.toString())
    }
    val virtualFile = element.containingFile?.virtualFile
    if (virtualFile != null) {
        info(
            "ideaModule",
            ifIndexReady { ModuleUtil.findModuleForFile(virtualFile, element.project)?.name ?: "null" } ?: "<index not ready>")
    }
}

private fun <T : Any> ifIndexReady(body: () -> T): T? = try {
    body()
} catch (e: IndexNotReadyException) {
    null
}

internal fun ResolutionFacade.createdFor(
    files: Collection<KtFile>,
    moduleInfo: ModuleInfo?,
    platform: TargetPlatform? = null
): ResolutionFacade {
    return ResolutionFacadeWithDebugInfo(this, CreationPlace(files, moduleInfo, platform))
}