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
import org.jetbrains.jet.lang.psi.codeFragmentUtil.skipVisibilityCheck
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.codeInsight.unwrap.RangeSplitter
import com.intellij.codeInsight.unwrap.UnwrapHandler
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import java.util.Collections
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.components.JBList
import com.intellij.openapi.ui.popup.JBPopupAdapter
import com.intellij.openapi.ui.popup.LightweightWindowEvent

/**
 * Replace [[JetSimpleNameExpression]] (and its enclosing qualifier) with qualified element given by FqName
 * Result is either the same as original element, or [[JetQualifiedExpression]], or [[JetUserType]]
 * Note that FqName may not be empty
 */
fun JetSimpleNameExpression.changeQualifiedName(fqName: FqName): JetElement {
    assert (!fqName.isRoot(), "Can't set empty FqName for element $this")

    val shortName = fqName.shortName().asString()
    val psiFactory = JetPsiFactory(this)
    val fqNameBase = (getParent() as? JetCallExpression)?.let { parent ->
        val callCopy = parent.copy() as JetCallExpression
        callCopy.getCalleeExpression()!!.replace(psiFactory.createSimpleName(shortName)).getParent()!!.getText()
    } ?: shortName

    val text = if (!fqName.isOneSegmentFQN()) "${fqName.parent().asString()}.$fqNameBase" else fqNameBase

    val elementToReplace = getQualifiedElement()
    return when (elementToReplace) {
        is JetUserType -> {
            val typeText = "$text${elementToReplace.getTypeArgumentList()?.getText() ?: ""}"
            elementToReplace.replace(psiFactory.createType(typeText).getTypeElement()!!)
        }
        else -> elementToReplace.replace(psiFactory.createExpression(text))
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
    val tmpFile = JetPsiFactory(this).createFile(getName(), textTransform(getText() ?: ""))
    tmpFile.setOriginalFile(this)
    tmpFile.skipVisibilityCheck = skipVisibilityCheck
    return tmpFile
}

public fun PsiElement.getAllExtractionContainers(strict: Boolean): List<JetElement> {
    val containers = ArrayList<JetElement>()

    var element: PsiElement? = if (strict) getParent() else this
    while (element != null) {
        when (element) {
            is JetBlockExpression, is JetClassBody, is JetFile -> containers.add(element as JetElement)
        }

        element = element!!.getParent()
    }

    return containers
}

public fun Project.checkConflictsInteractively(conflicts: MultiMap<PsiElement, String>, onAccept: () -> Unit) {
    if (!conflicts.isEmpty()) {
        if (ApplicationManager.getApplication()!!.isUnitTestMode()) throw ConflictsInTestsException(conflicts.values())

        val dialog = ConflictsDialog(this, conflicts, onAccept)
        dialog.show()
        if (!dialog.isOK()) return
    }

    onAccept()
}

public fun runReadAction<T: Any>(action: () -> T?): T? {
    return ApplicationManager.getApplication()?.runReadAction<T>(action)
}

public fun runWriteAction<T: Any>(action: () -> T?): T? {
    return ApplicationManager.getApplication()?.runWriteAction<T>(action)
}

public fun Project.executeWriteCommand(name: String, command: () -> Unit) {
    CommandProcessor.getInstance().executeCommand(this, { runWriteAction(command) }, name, null)
}

public fun <T : PsiElement> getPsiElementPopup(
        editor: Editor,
        elements: Array<T>,
        renderer: PsiElementListCellRenderer<T>,
        title: String? = null,
        processor: (T) -> Boolean): JBPopup {
    val highlighter = SelectionAwareScopeHighlighter(editor)

    val list = JBList(elements.toList())
    list.setCellRenderer(renderer)
    list.addListSelectionListener { e ->
        highlighter.dropHighlight()
        val index = list.getSelectedIndex()
        if (index >= 0) {
            highlighter.highlight(list.getModel()!!.getElementAt(index) as PsiElement)
        }
    }

    return with(PopupChooserBuilder(list)) {
        title?.let { setTitle(it) }
        renderer.installSpeedSearch(this, true)
        setItemChoosenCallback {
            for (element in list.getSelectedValues()) {
                element.let {
                    [suppress("UNCHECKED_CAST")]
                    processor(it as T)
                }
            }
        }
        addListener(object: JBPopupAdapter() {
            override fun onClosed(event: LightweightWindowEvent?) {
                highlighter.dropHighlight();
            }
        })

        createPopup()
    }
}

public class SelectionAwareScopeHighlighter(val editor: Editor) {
    private val highlighters = ArrayList<RangeHighlighter>()

    private fun addHighlighter(r: TextRange, attr: TextAttributes) {
        highlighters.add(
                editor.getMarkupModel().addRangeHighlighter(
                        r.getStartOffset(),
                        r.getEndOffset(),
                        UnwrapHandler.HIGHLIGHTER_LEVEL,
                        attr,
                        HighlighterTargetArea.EXACT_RANGE
                )
        )
    }

    public fun highlight(wholeAffected: PsiElement) {
        dropHighlight()

        val attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)!!
        val selectedRange = with(editor.getSelectionModel()) { TextRange(getSelectionStart(), getSelectionEnd()) }
        for (r in RangeSplitter.split(wholeAffected.getTextRange()!!, Collections.singletonList(selectedRange))) {
            addHighlighter(r, attributes)
        }
    }

    public fun dropHighlight() {
        highlighters.forEach { it.dispose() }
        highlighters.clear()
    }
}