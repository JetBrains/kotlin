package org.jetbrains.konan

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService

fun createFileStub(project: Project, text: String): PsiFileStub<*> {
    val virtualFile = LightVirtualFile("dummy.kt", KotlinFileType.INSTANCE, text)
    virtualFile.language = KotlinLanguage.INSTANCE
    SingleRootFileViewProvider.doNotCheckFileSizeLimit(virtualFile)

    val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
    val file = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, false, false)!!
    return KtStubElementTypes.FILE.builder.buildStubTree(file) as PsiFileStub<*>
}

fun createLoggingErrorReporter(log: Logger) = LoggingErrorReporter(log)

fun <M : ModuleInfo> destructModuleContent(moduleContent: ModuleContent<M>) =
    moduleContent.syntheticFiles to moduleContent.moduleContentScope

fun <M : ModuleInfo> createDeclarationProviderFactory(
    project: Project,
    moduleContext: ModuleContext,
    syntheticFiles: Collection<KtFile>,
    moduleInfo: M,
    globalSearchScope: GlobalSearchScope?
) = DeclarationProviderFactoryService.createDeclarationProviderFactory(
    project,
    moduleContext.storageManager,
    syntheticFiles,
    globalSearchScope!!,
    moduleInfo
)
