package org.jetbrains.konan.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiManager
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.cpp.OCIncludeSymbol
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile

class KotlinFrameworkTranslator(val project: Project) {
    fun translateModule(konanFile: KonanBridgeVirtualFile): Sequence<OCSymbol> {
        val sourceRoot = findSourceRoot(konanFile) ?: return emptySequence()

        val sources = findAllSources(sourceRoot)
        if (sources.isEmpty()) return emptySequence()

        val psiManager = PsiManager.getInstance(project)

        val ktFile = sources.asSequence().mapToKtFiles(psiManager).firstOrNull() ?: return emptySequence()

        val baseDeclarations = KotlinFileTranslator(project).translateBase(ktFile)
        val includes = sources.asSequence().map { include(konanFile, it) }

        return baseDeclarations + includes
    }

    private fun findSourceRoot(virtualFile: KonanBridgeVirtualFile): VirtualFile? {
        /*todo[medvedev] implement proper module stuff here
        ExternalSystemApiUtil.findAll(ProjectDataManager.getInstance().getExternalProjectsData(virtualFile.project, GradleConstants.SYSTEM_ID).first().externalProjectStructure as DataNode<*>, ProjectKeys.MODULE)
        ask Vlad Soroka what should I get next*/
        return virtualFile.target.file.baseDir
                .findChild("KotlinNativeFramework")
                ?.findChild("src")
                ?.findChild("KotlinNativeFrameworkMain")
                ?.findChild("kotlin")
    }

    private fun findAllSources(sourceRoot: VirtualFile): List<VirtualFile> {
        val result = arrayListOf<VirtualFile>()

        VfsUtilCore.visitChildrenRecursively(sourceRoot, object : VirtualFileVisitor<Nothing>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (file.fileType == KotlinFileType.INSTANCE) {
                    result.add(file)
                }
                return true
            }
        })

        return result
    }

    private fun include(konanFile: KonanBridgeVirtualFile, target: VirtualFile): OCIncludeSymbol {
        return OCIncludeSymbol(konanFile, 0, target, OCIncludeSymbol.IncludePath.EMPTY, true, false, 0, null, true)
    }

    private fun Sequence<VirtualFile>.mapToKtFiles(psiManager: PsiManager): Sequence<KtFile> {
        return this.mapNotNull { file -> psiManager.findFile(file) as? KtFile }
    }
}
