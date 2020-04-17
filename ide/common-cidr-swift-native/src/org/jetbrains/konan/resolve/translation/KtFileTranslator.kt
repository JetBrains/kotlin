package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MostlySingularMultiMap
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import org.jetbrains.konan.resolve.symbols.KtSymbol
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.util.getValueOrNull

abstract class KtFileTranslator<T : KtSymbol, M : OCSymbol> {
    fun translate(file: KtFile, frameworkName: String): List<T> {
        val (stubTrace, lazy) = createStubProvider(file, frameworkName)
        return translate(stubTrace, lazy.translate(file), file.virtualFile)
    }

    fun translateBase(file: KtFile, frameworkName: String): List<T> {
        val (stubTrace, lazy) = createStubProvider(file, frameworkName)
        return translate(stubTrace, lazy.generateBase(), file.virtualFile)
    }

    fun translateMembers(stub: ObjCClass<*>, clazz: T): MostlySingularMultiMap<String, M>? {
        val map = lazy(LazyThreadSafetyMode.NONE) { MostlySingularMultiMap<String, M>() }
        for (member in stub.members) {
            translateMember(member, clazz, clazz.containingFile) {
                map.value.add(it.name, it)
            }
        }
        return map.getValueOrNull()
    }

    protected abstract fun translate(stubTrace: StubTrace, stubs: Collection<ObjCTopLevel<*>>, file: VirtualFile): List<T>
    protected abstract fun translateMember(stub: Stub<*>, clazz: T, file: VirtualFile, processor: (M) -> Unit)

    private fun createStubProvider(file: KtFile, frameworkName: String): Pair<StubTrace, ObjCExportLazy> {
        val configuration = object : ObjCExportLazy.Configuration {
            override val frameworkName: String get() = frameworkName

            override fun getCompilerModuleName(moduleInfo: ModuleInfo): String =
                TODO() // no implementation in `KonanCompilerFrontendServices.kt` either

            override fun isIncluded(moduleInfo: ModuleInfo): Boolean =
                true // always return true in `KonanCompilerFrontendServices.kt` as well

            override val objcGenerics: Boolean get() = false
        }

        val resolutionFacade = file.getResolutionFacade()
        val moduleDescriptor = resolutionFacade.moduleDescriptor
        val resolveSession = resolutionFacade.frontendService<ResolveSession>()
        val deprecationResolver = resolutionFacade.frontendService<DeprecationResolver>()

        val stubTrace = StubTrace(
            file.virtualFile,
            resolutionFacade,
            moduleDescriptor
        )

        return stubTrace to createObjCExportLazy(
            configuration,
            ObjCExportWarningCollector.SILENT,
            resolveSession,
            resolveSession.typeResolver,
            resolveSession.descriptorResolver,
            resolveSession.fileScopeProvider,
            moduleDescriptor.builtIns,
            deprecationResolver
        )
    }

    companion object {
        internal fun isSupported(context: OCInclusionContext): Boolean =
            context.languageKind.let { it == SwiftLanguageKind || it is CLanguageKind }

        internal fun createTranslator(context: OCInclusionContext): KtFileTranslator<*, *> = when (context.languageKind) {
            SwiftLanguageKind -> KtSwiftSymbolTranslator(context.project)
            is CLanguageKind -> KtOCSymbolTranslator(context.project)
            else -> throw UnsupportedOperationException("Unsupported language kind ${context.languageKind}")
        }
    }
}