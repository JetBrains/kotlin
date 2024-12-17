package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.CoreStubTreeLoader
import com.intellij.psi.stubs.ObjectStubTree
import com.intellij.psi.stubs.StubTreeLoader

class StubTreeLoaderImpl : StubTreeLoader() {

    override fun readOrBuild(project: Project, vFile: VirtualFile, psiFile: PsiFile?): ObjectStubTree<*>? =
        project.getService(StubIndexProjectService::class.java).stubIndex.stub(vFile)

    override fun build(project: Project?, vFile: VirtualFile, psiFile: PsiFile?): ObjectStubTree<*>? =
        throw UnsupportedOperationException("why do you need this?")

    override fun readFromVFile(project: Project, vFile: VirtualFile): ObjectStubTree<*>? =
        project.getService(StubIndexProjectService::class.java).stubIndex.stub(vFile)

    override fun rebuildStubTree(virtualFile: VirtualFile?) {
        TODO("Not yet implemented")
    }

    override fun canHaveStub(file: VirtualFile?): Boolean {
        return CoreStubTreeLoader().canHaveStub(file)
    }
}