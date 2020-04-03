package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.konan.resolve.KtPendingTranslationsManager
import org.jetbrains.konan.resolve.KtPendingTranslationsRegistry
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade

class StubTrace(
    internal val file: VirtualFile,
    private val resolutionFacade: ResolutionFacade,
    private val moduleDescriptorTrace: ModuleDescriptor
) {
    val project: Project
        get() = resolutionFacade.project

    internal val pendingRegistry: KtPendingTranslationsRegistry =
        KtPendingTranslationsManager.getInstance(project).getPendingTranslationsRegistry(moduleDescriptorTrace)

    val isValid: Boolean
        get() = when {
            project.isDisposed || !moduleDescriptorTrace.isValid || resolutionFacade.moduleDescriptor !== moduleDescriptorTrace -> false
            else -> true.also { assert(moduleDescriptorTrace.isValid) { "Resolution facade returned invalid module descriptor" } }
        }
}

data class TranslationState<Stb : Stub<*>>(val stubTrace: StubTrace, val stub: Stb) {
    init {
        stubTrace.pendingRegistry.newPendingTranslation(stubTrace.file)
    }

    fun didCompleteSuccessfully() {
        stubTrace.pendingRegistry.successfullyCompletedTranslation(stubTrace.file)
    }

    val project: Project
        get() = stubTrace.project
}