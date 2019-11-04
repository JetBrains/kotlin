package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.OCSymbol
import org.jetbrains.konan.resolve.konan.KonanTarget
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

class KtFileTranslator(val project: Project) {
    private val stubToOCSymbolTranslator = StubToOCSymbolTranslator(project)
    private val stubToSwiftSymbolTranslator = StubToSwiftSymbolTranslator(project)

    fun translate(file: KtFile, target: KonanTarget): Sequence<OCSymbol> = createStubProvider(file, target).translate(file).asSequence().translate(file.virtualFile)
    fun translateBase(file: KtFile, target: KonanTarget): Sequence<OCSymbol> = createStubProvider(file, target).generateBase().asSequence().translate(file.virtualFile)

    private fun Sequence<Stub<*>>.translate(file: VirtualFile): Sequence<OCSymbol> = mapNotNull { stubToOCSymbolTranslator.translate(it, file) } +
                                                                                     mapNotNull { stubToSwiftSymbolTranslator.translate(it, file) }

    private fun createStubProvider(file: KtFile, target: KonanTarget): ObjCExportLazy {
        val configuration = object : ObjCExportLazy.Configuration {
            override val frameworkName: String get() = target.productModuleName
            override fun getCompilerModuleName(moduleInfo: ModuleInfo): String = frameworkName //todo[medvedev] what should I return here???
            override fun isIncluded(moduleInfo: ModuleInfo): Boolean = true //todo[medvedev] what should I return here???
            override val objcGenerics: Boolean get() = false
        }

        val resolutionFacade = file.getResolutionFacade()
        val resolveSession = resolutionFacade.getFrontendService(ResolveSession::class.java)
        return createObjCExportLazy(
            configuration,
            SilentWarningCollector,
            resolveSession,
            resolveSession.typeResolver,
            resolveSession.descriptorResolver,
            resolveSession.fileScopeProvider,
            resolveSession.moduleDescriptor.builtIns,
            resolutionFacade.frontendService()
        )
    }

    private object SilentWarningCollector : ObjCExportWarningCollector {
        override fun reportWarning(text: String) {}
        override fun reportWarning(method: FunctionDescriptor, text: String) {}
    }
}