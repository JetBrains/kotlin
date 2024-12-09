package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.CoreStubTreeLoader
import com.intellij.psi.stubs.ObjectStubTree
import com.intellij.psi.stubs.StubTreeLoader

class StubTreeLoaderImpl : StubTreeLoader() {
    lateinit var stubIndex: StubIndex

    override fun readOrBuild(project: Project, vFile: VirtualFile, psiFile: PsiFile?): ObjectStubTree<*>? =
        stubIndex.stub(vFile)

    override fun build(project: Project?, vFile: VirtualFile, psiFile: PsiFile?): ObjectStubTree<*>? =
        stubIndex.stub(vFile)

    override fun readFromVFile(project: Project, vFile: VirtualFile): ObjectStubTree<*>? =
        stubIndex.stub(vFile)

    override fun rebuildStubTree(virtualFile: VirtualFile?) {
        TODO("Not yet implemented")
    }

    override fun canHaveStub(file: VirtualFile?): Boolean {
        return CoreStubTreeLoader().canHaveStub(file)
    }
}