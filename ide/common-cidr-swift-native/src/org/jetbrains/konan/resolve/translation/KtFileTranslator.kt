package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.MostlySingularMultiMap
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import gnu.trove.TObjectLongHashMap
import org.jetbrains.konan.resolve.symbols.KtDependencyMarker
import org.jetbrains.konan.resolve.symbols.KtSymbol
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.util.getValueOrNull

abstract class KtFileTranslator<T : KtSymbol, M : OCSymbol> {
    fun translate(file: KtFile, frameworkName: String, destination: MutableList<in KtSymbol>) {
        translate(file, frameworkName, destination, ObjCExportLazy::translate)
    }

    fun translateBase(file: KtFile, frameworkName: String, destination: MutableList<in KtSymbol>) {
        translate(file, frameworkName, destination) { lazy, _ -> lazy.generateBase() }
    }

    fun translateMembers(containingStub: ObjCClass<*>, project: Project, containingClass: T): MostlySingularMultiMap<String, M>? {
        val map = lazy(LazyThreadSafetyMode.NONE) { MostlySingularMultiMap<String, M>() }
        for (member in containingStub.members) {
            translateMember(member, project, containingClass.containingFile, containingClass) {
                map.value.add(it.name, it)
            }
        }
        return map.getValueOrNull()
    }

    protected abstract fun translate(
        stubTrace: StubTrace, stubs: Collection<ObjCTopLevel<*>>, file: VirtualFile, destination: MutableList<in T>
    )

    protected abstract fun translateMember(stub: Stub<*>, project: Project, file: VirtualFile, containingClass: T, processor: (M) -> Unit)

    private class ObjCExportConfiguration(override val frameworkName: String) : ObjCExportLazy.Configuration {
        override fun getCompilerModuleName(moduleInfo: ModuleInfo): String =
            TODO() // no implementation in `KonanCompilerFrontendServices.kt` either

        override fun isIncluded(moduleInfo: ModuleInfo): Boolean =
            true // always return true in `KonanCompilerFrontendServices.kt` as well

        override val objcGenerics: Boolean get() = false
    }

    private inline fun translate(
        file: KtFile, frameworkName: String, destination: MutableList<in KtSymbol>,
        provideStubs: (ObjCExportLazy, KtFile) -> List<ObjCTopLevel<*>>
    ) {
        val resolutionFacade = file.getResolutionFacade()
        val moduleDescriptor = resolutionFacade.moduleDescriptor
        val resolveSession = resolutionFacade.frontendService<ResolveSession>()
        val deprecationResolver = resolutionFacade.frontendService<DeprecationResolver>()

        val lazy = createObjCExportLazy(
            ObjCExportConfiguration(frameworkName),
            ObjCExportWarningCollector.SILENT,
            resolveSession,
            resolveSession.typeResolver,
            resolveSession.descriptorResolver,
            resolveSession.fileScopeProvider,
            moduleDescriptor.builtIns,
            deprecationResolver
        )

        val virtualFile = file.virtualFile
        val dependencies = determineDependencies(virtualFile, moduleDescriptor)
        translate(StubTrace(virtualFile, resolutionFacade, moduleDescriptor), provideStubs(lazy, file), virtualFile, destination)
        destination += KtDependencyMarker(virtualFile, dependencies)
    }

    companion object {
        @JvmField
        val PRELOADED_LANGUAGE_KINDS: Collection<OCLanguageKind> = listOf(CLanguageKind.OBJ_C, SwiftLanguageKind)

        @JvmStatic
        val OCInclusionContext.isKtTranslationSupported: Boolean
            get() = languageKind.let { it == SwiftLanguageKind || it is CLanguageKind }

        @JvmStatic
        val OCInclusionContext.ktTranslator: KtFileTranslator<*, *>
            get() = when (languageKind) {
                SwiftLanguageKind -> KtSwiftSymbolTranslator
                is CLanguageKind -> KtOCSymbolTranslator
                else -> throw UnsupportedOperationException("Unsupported language kind $languageKind")
            }

        //TODO: more precise dependency tracking via LookupTracker
        //TODO: track changes in libraries?
        private fun determineDependencies(virtualFile: VirtualFile?, moduleDescriptor: ModuleDescriptor): TObjectLongHashMap<VirtualFile> =
            TObjectLongHashMap<VirtualFile>().apply {
                val moduleInfo = moduleDescriptor.getCapability(ModuleInfo.Capability)?.unwrapModuleSourceInfo() ?: return@apply
                val scope = GlobalSearchScope.union(
                    moduleInfo.dependencies().mapNotNull { it.unwrapModuleSourceInfo()?.contentScope() }.toTypedArray()
                )
                for (depFile in FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)) {
                    if (depFile !== virtualFile) put(depFile, depFile.modificationStamp)
                }
            }
    }
}