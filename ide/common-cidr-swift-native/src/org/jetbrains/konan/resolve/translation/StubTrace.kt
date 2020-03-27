package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade

data class StubTrace(val resolutionFacade: ResolutionFacade, val moduleDescriptorTrace: ModuleDescriptor) {
    val project: Project
        get() = resolutionFacade.project
}

data class StubAndProject<Stb : Stub<*>>(val stubTrace: StubTrace, val stub: Stb) {
    val project: Project
        get() = stubTrace.project
}