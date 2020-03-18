package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MostlySingularMultiMap
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import org.jetbrains.konan.resolve.konan.KonanTarget
import org.jetbrains.konan.resolve.symbols.KtSymbol
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.util.getValueOrNull

abstract class KtFileTranslator<T : KtSymbol, M : OCSymbol> {
    fun translate(file: KtFile, target: KonanTarget): Sequence<T> {
        val (moduleDescriptor, lazy) = createStubProvider(file, target)
        return lazy.translate(file).translate(moduleDescriptor, file.virtualFile)
    }

    fun translateBase(file: KtFile, target: KonanTarget): Sequence<T> {
        val (moduleDescriptor, lazy) = createStubProvider(file, target)
        return lazy.generateBase().translate(moduleDescriptor, file.virtualFile)
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

    private fun Collection<ObjCTopLevel<*>>.translate(moduleDescriptor: ModuleDescriptor, file: VirtualFile): Sequence<T> =
        asSequence().mapNotNull { translate(moduleDescriptor, it, file) }

    protected abstract fun translate(moduleDescriptor: ModuleDescriptor, stub: ObjCTopLevel<*>, file: VirtualFile): T?
    protected abstract fun translateMember(stub: Stub<*>, clazz: T, file: VirtualFile, processor: (M) -> Unit)

    private fun createStubProvider(file: KtFile, target: KonanTarget): Pair<ModuleDescriptor, ObjCExportLazy> {
        val configuration = object : ObjCExportLazy.Configuration {
            override val frameworkName: String get() = target.productModuleName

            override fun getCompilerModuleName(moduleInfo: ModuleInfo): String =
                TODO() // no implementation in `KonanCompilerFrontendServices.kt` either

            override fun isIncluded(moduleInfo: ModuleInfo): Boolean =
                true // always return true in `KonanCompilerFrontendServices.kt` as well

            override val objcGenerics: Boolean get() = false
        }

        val resolutionFacade = file.getResolutionFacade()
        val resolveSession = resolutionFacade.getFrontendService(ResolveSession::class.java)
        return resolutionFacade.moduleDescriptor to createObjCExportLazy(
            configuration,
            ObjCExportWarningCollector.SILENT,
            resolveSession,
            resolveSession.typeResolver,
            resolveSession.descriptorResolver,
            resolveSession.fileScopeProvider,
            resolveSession.moduleDescriptor.builtIns,
            resolutionFacade.frontendService()
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