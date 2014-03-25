/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.refactoring

import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElement
import org.jetbrains.jet.lang.resolve.name.isOneSegmentFQN
import com.intellij.psi.PsiElement
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDirectory
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.jet.asJava.namedUnwrappedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.ConflictsUtil
import org.jetbrains.jet.lang.psi.psiUtil.getPackage
import com.intellij.psi.PsiFileFactory
import org.jetbrains.jet.plugin.JetFileType
import com.intellij.openapi.project.Project
import java.io.File
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.lang.psi.*
import java.util.ArrayList
import com.intellij.openapi.application.ApplicationManager
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.refactoring.ui.ConflictsDialog
import com.intellij.util.containers.MultiMap
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.plugin.codeInsight.TipsManager
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache

/**
 * Replace [[JetSimpleNameExpression]] (and its enclosing qualifier) with qualified element given by FqName
 * Result is either the same as original element, or [[JetQualifiedExpression]], or [[JetUserType]]
 * Note that FqName may not be empty
 */
fun JetSimpleNameExpression.changeQualifiedName(fqName: FqName): JetElement {
    assert (!fqName.isRoot(), "Can't set empty FqName for element $this")

    val project = getProject()

    val shortName = fqName.shortName().asString()
    val fqNameBase = (getParent() as? JetCallExpression)?.let { parent ->
        val callCopy = parent.copy() as JetCallExpression
        callCopy.getCalleeExpression()!!.replace(JetPsiFactory.createSimpleName(project, shortName)).getParent()!!.getText()
    } ?: shortName

    val text = if (!fqName.isOneSegmentFQN()) "${fqName.parent().asString()}.$fqNameBase" else fqNameBase

    val elementToReplace = getQualifiedElement()
    return when (elementToReplace) {
        is JetUserType -> elementToReplace.replace(JetPsiFactory.createType(project, text).getTypeElement()!!)
        else -> elementToReplace.replace(JetPsiFactory.createExpression(project, text))
    } as JetElement
}

fun <T: Any> PsiElement.getAndRemoveCopyableUserData(key: Key<T>): T? {
    val data = getCopyableUserData(key)
    putCopyableUserData(key, null)
    return data
}

fun createKotlinFile(fileName: String, targetDir: PsiDirectory): JetFile {
    val packageName = targetDir.getPackage()?.getQualifiedName()

    targetDir.checkCreateFile(fileName)
    val file = PsiFileFactory.getInstance(targetDir.getProject())!!.createFileFromText(
            fileName, JetFileType.INSTANCE, if (packageName != null) "package $packageName \n\n" else ""
    )

    return targetDir.add(file) as JetFile
}

public fun File.toVirtualFile(): VirtualFile? = LocalFileSystem.getInstance()!!.findFileByIoFile(this)

public fun File.toPsiFile(project: Project): PsiFile? {
    return toVirtualFile()?.let { vfile -> PsiManager.getInstance(project).findFile(vfile) }
}

/**
 * Returns FqName for given declaration (either Java or Kotlin)
 */
public fun PsiElement.getKotlinFqName(): FqName? {
    val element = namedUnwrappedElement
    return when (element) {
        is PsiPackage -> FqName(element.getQualifiedName())
        is PsiClass -> element.getQualifiedName()?.let { FqName(it) }
        is PsiMember -> (element : PsiMember).getName()?.let { name ->
            val prefix = element.getContainingClass()?.getQualifiedName()
            FqName(if (prefix != null) "$prefix.$name" else name)
        }
        is JetNamedDeclaration -> element.getFqName()
        else -> null
    }
}

public fun PsiElement.getUsageContext(): PsiElement {
    return when (this) {
        is JetElement -> PsiTreeUtil.getParentOfType(this, javaClass<JetNamedDeclaration>(), javaClass<JetFile>())!!
        else -> ConflictsUtil.getContainer(this)
    }
}

public fun PsiElement.isInJavaSourceRoot(): Boolean =
        !JavaProjectRootsUtil.isOutsideJavaSourceRoot(getContainingFile())

public inline fun JetFile.createTempCopy(textTransform: (String) -> String): JetFile {
    val tmpFile = JetPsiFactory.createFile(getProject(), getName(), textTransform(getText() ?: ""))
    tmpFile.setOriginalFile(this)
    return tmpFile
}

