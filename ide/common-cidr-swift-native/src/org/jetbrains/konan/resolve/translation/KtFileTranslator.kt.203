package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.containers.MostlySingularMultiMap
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import org.jetbrains.konan.resolve.symbols.KtDependencyMap
import org.jetbrains.konan.resolve.symbols.KtDependencyMarker
import org.jetbrains.konan.resolve.symbols.KtSymbol
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getPlatformModuleInfo
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
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

    class ObjCExportConfiguration(override val frameworkName: String) : ObjCExportLazy.Configuration {
        override fun getCompilerModuleName(moduleInfo: ModuleInfo): String =
            TODO() // no implementation in `KonanCompilerFrontendServices.kt` either

        override fun isIncluded(moduleInfo: ModuleInfo): Boolean =
            true // always return true in `KonanCompilerFrontendServices.kt` as well

        override val objcGenerics: Boolean get() = false
    }

    @OptIn(FrontendInternals::class)
    private inline fun translate(
        file: KtFile, frameworkName: String, destination: MutableList<in KtSymbol>,
        provideStubs: (ObjCExportLazy, KtFile) -> List<ObjCTopLevel<*>>
    ) {
        val (moduleInfo, platform) = file.getIOSModuleInfoAndPlatform()
        val resolutionFacade = KotlinCacheService.getInstance(file.project).getResolutionFacadeByModuleInfo(moduleInfo, platform)!!
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
        val dependencies = determineDependencies(virtualFile, moduleInfo.unwrapModuleSourceInfo())
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
        fun determineDependencies(virtualFile: VirtualFile, moduleInfo: ModuleSourceInfo?): KtDependencyMap =
            KtDependencyMap().apply {
                processDependencies(virtualFile, moduleInfo ?: return@apply) {
                    put(it, it.modificationStamp)
                    true
                }
            }

        fun verifyDependencies(virtualFile: VirtualFile, moduleInfo: ModuleSourceInfo?, dependencies: KtDependencyMap): Boolean {
            if (moduleInfo == null) return dependencies.isEmpty
            var count = 0
            return processDependencies(virtualFile, moduleInfo) {
                count++
                dependencies[it] == it.modificationStamp
            } && dependencies.size() == count
        }

        private inline fun processDependencies(
            virtualFile: VirtualFile, moduleInfo: ModuleSourceInfo,
            crossinline processor: (VirtualFile) -> Boolean
        ): Boolean = processAllModuleDependencies(moduleInfo, Processor {
            if (it == virtualFile) true else processor(it)
        })

        private fun processAllModuleDependencies(moduleInfo: ModuleSourceInfo, processor: Processor<VirtualFile>): Boolean {
            val scope = GlobalSearchScope.union(
                moduleInfo.dependencies().mapNotNull { it.unwrapModuleSourceInfo()?.contentScope() }.toTypedArray()
            )
            return FileTypeIndex.processFiles(KotlinFileType.INSTANCE, processor, scope)
        }

        //TODO properly pass target platform from gradle build file to referenced modules
        private fun KtFile.getIOSModuleInfoAndPlatform(): Pair<IdeaModuleInfo, TargetPlatform> {
            for (target in arrayOf(KonanTarget.IOS_X64, KonanTarget.IOS_ARM64, KonanTarget.IOS_ARM32)) { // HACK: search for iOS module
                val platform = NativePlatforms.nativePlatformBySingleTarget(target)
                getPlatformModuleInfo(platform)?.let { return it to platform }
            }
            return getModuleInfo() to NativePlatforms.unspecifiedNativePlatform
        }
    }
}