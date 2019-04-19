package org.jetbrains.konan.resolve

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.symbols.OCSymbol
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

class KotlinFileTranslator(val project: Project) {
    private val stubToSymbolTranslator = StubToSymbolTranslator(project)

    fun translate(file: KtFile): Sequence<OCSymbol> = createStubProvider(file).translate(file).asSequence().translate()
    fun translateBase(file: KtFile): Sequence<OCSymbol> = createStubProvider(file).generateBase().asSequence().translate()

    private fun Sequence<Stub<*>>.translate(): Sequence<OCSymbol> = mapNotNull { stubToSymbolTranslator.translate(it) }

    private fun createStubProvider(file: KtFile): ObjCExportLazyImpl {
        val configuration = object : ObjCExportLazy.Configuration {
            override val frameworkName: String get() = "KotlinNativeFramework" //todo[medvedev] infer framework name. it equals xcodeTarget.productModuleName
            override fun getCompilerModuleName(moduleInfo: ModuleInfo): String = "KotlinNativeFramework" //todo[medvedev] what should I return here???
            override fun isIncluded(moduleInfo: ModuleInfo): Boolean = true //todo[medvedev] what should I return here???
        }

        val resolveSession = file.getResolutionFacade().getFrontendService(ResolveSession::class.java)
        val typeResolver = resolveSession.typeResolver
        val descriptorResolver = resolveSession.descriptorResolver
        val fileScopeProvider = resolveSession.fileScopeProvider

        return ObjCExportLazyImpl(configuration, resolveSession, typeResolver, descriptorResolver, fileScopeProvider)
    }


}