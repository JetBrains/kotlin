/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.core.refactoring

import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils
import com.intellij.codeInsight.unwrap.RangeSplitter
import com.intellij.codeInsight.unwrap.UnwrapHandler
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupAdapter
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.refactoring.changeSignature.ChangeSignatureUtil
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.refactoring.ui.ConflictsDialog
import com.intellij.refactoring.util.ConflictsUtil
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.ui.components.JBList
import com.intellij.util.VisibilityUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.string.collapseSpaces
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.JavaToKotlinConverter
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File
import java.lang.annotation.Retention
import java.util.ArrayList
import java.util.Collections
import javax.swing.Icon

fun <T: Any> PsiElement.getAndRemoveCopyableUserData(key: Key<T>): T? {
    val data = getCopyableUserData(key)
    putCopyableUserData(key, null)
    return data
}

fun getOrCreateKotlinFile(fileName: String, targetDir: PsiDirectory): JetFile? =
        (targetDir.findFile(fileName) ?: createKotlinFile(fileName, targetDir)) as? JetFile

fun createKotlinFile(fileName: String, targetDir: PsiDirectory): JetFile {
    val packageName = targetDir.getPackage()?.getQualifiedName()

    targetDir.checkCreateFile(fileName)
    val file = PsiFileFactory.getInstance(targetDir.getProject()).createFileFromText(
            fileName, JetFileType.INSTANCE, if (packageName != null && packageName.isNotEmpty()) "package $packageName \n\n" else ""
    )

    return targetDir.add(file) as JetFile
}

public fun File.toVirtualFile(): VirtualFile? = LocalFileSystem.getInstance().findFileByIoFile(this)

public fun File.toPsiFile(project: Project): PsiFile? = toVirtualFile()?.toPsiFile(project)

public fun File.toPsiDirectory(project: Project): PsiDirectory? {
    return toVirtualFile()?.let { vfile -> PsiManager.getInstance(project).findDirectory(vfile) }
}

public fun VirtualFile.toPsiFile(project: Project): PsiFile? = PsiManager.getInstance(project).findFile(this)

public fun PsiElement.getUsageContext(): PsiElement {
    return when (this) {
        is JetElement -> PsiTreeUtil.getParentOfType(this, javaClass<JetNamedDeclaration>(), javaClass<JetFile>())!!
        else -> ConflictsUtil.getContainer(this)
    }
}

public fun PsiElement.isInJavaSourceRoot(): Boolean =
        !JavaProjectRootsUtil.isOutsideJavaSourceRoot(getContainingFile())

public inline fun JetFile.createTempCopy(textTransform: (String) -> String): JetFile {
    val tmpFile = JetPsiFactory(this).createAnalyzableFile(getName(), textTransform(getText() ?: ""), this)
    tmpFile.setOriginalFile(this)
    tmpFile.suppressDiagnosticsInDebugMode = suppressDiagnosticsInDebugMode
    return tmpFile
}

public fun PsiElement.getAllExtractionContainers(strict: Boolean = true): List<JetElement> {
    val containers = ArrayList<JetElement>()

    var element: PsiElement? = if (strict) getParent() else this
    while (element != null) {
        when (element) {
            is JetBlockExpression, is JetClassBody, is JetFile -> containers.add(element as JetElement)
        }

        element = element.getParent()
    }

    return containers
}

public fun PsiElement.getExtractionContainers(strict: Boolean = true, includeAll: Boolean = false): List<JetElement> {
    fun getEnclosingDeclaration(element: PsiElement, strict: Boolean): PsiElement? {
        return (if (strict) element.parents else element.parentsWithSelf)
                .filter {
                    (it is JetDeclarationWithBody && it !is JetFunctionLiteral)
                    || it is JetClassInitializer
                    || it is JetClassBody
                    || it is JetFile
                }
                .firstOrNull()
    }

    if (includeAll) return getAllExtractionContainers(strict)

    val enclosingDeclaration = getEnclosingDeclaration(this, strict)?.let {
        if (it is JetDeclarationWithBody || it is JetClassInitializer) getEnclosingDeclaration(it, true) else it
    }

    return when (enclosingDeclaration) {
        is JetFile -> Collections.singletonList(enclosingDeclaration)
        is JetClassBody -> getAllExtractionContainers(strict).filterIsInstance<JetClassBody>()
        else -> {
            val targetContainer = when (enclosingDeclaration) {
                is JetDeclarationWithBody -> enclosingDeclaration.getBodyExpression()
                is JetClassInitializer -> enclosingDeclaration.getBody()
                else -> null
            }
            if (targetContainer is JetBlockExpression) Collections.singletonList(targetContainer) else Collections.emptyList()
        }
    }
}

public fun Project.checkConflictsInteractively(
        conflicts: MultiMap<PsiElement, String>,
        onShowConflicts: () -> Unit = {},
        onAccept: () -> Unit) {
    if (!conflicts.isEmpty()) {
        if (ApplicationManager.getApplication()!!.isUnitTestMode()) throw ConflictsInTestsException(conflicts.values())

        val dialog = ConflictsDialog(this, conflicts) { onAccept() }
        dialog.show()
        if (!dialog.isOK()) {
            if (dialog.isShowConflicts()) {
                onShowConflicts()
            }
            return
        }
    }

    onAccept()
}

public fun reportDeclarationConflict(
        conflicts: MultiMap<PsiElement, String>,
        declaration: PsiElement,
        message: (renderedDeclaration: String) -> String
) {
    conflicts.putValue(declaration, message(RefactoringUIUtil.getDescription(declaration, true).capitalize()))
}

public fun <T, E: PsiElement> getPsiElementPopup(
        editor: Editor,
        elements: List<T>,
        renderer: PsiElementListCellRenderer<E>,
        title: String?,
        highlightSelection: Boolean,
        toPsi: (T) -> E,
        processor: (T) -> Boolean): JBPopup {
    val highlighter = if (highlightSelection) SelectionAwareScopeHighlighter(editor) else null

    val list = JBList(elements.map(toPsi))
    list.setCellRenderer(renderer)
    list.addListSelectionListener { e ->
        highlighter?.dropHighlight()
        val index = list.getSelectedIndex()
        if (index >= 0) {
            highlighter?.highlight(list.getModel()!!.getElementAt(index) as PsiElement)
        }
    }

    return with(PopupChooserBuilder(list)) {
        title?.let { setTitle(it) }
        renderer.installSpeedSearch(this, true)
        setItemChoosenCallback {
            val index = list.getSelectedIndex()
            if (index >= 0) {
                processor(elements[index])
            }
        }
        addListener(object: JBPopupAdapter() {
            override fun onClosed(event: LightweightWindowEvent?) {
                highlighter?.dropHighlight();
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

fun PsiElement.getLineCount(): Int {
    val doc = getContainingFile()?.let { file -> PsiDocumentManager.getInstance(getProject()).getDocument(file) }
    if (doc != null) {
        val spaceRange = getTextRange() ?: TextRange.EMPTY_RANGE

        val startLine = doc.getLineNumber(spaceRange.getStartOffset())
        val endLine = doc.getLineNumber(spaceRange.getEndOffset())

        return endLine - startLine
    }

    return (getText() ?: "").count { it == '\n' } + 1
}

fun PsiElement.isMultiLine(): Boolean = getLineCount() > 1

public fun JetElement.getContextForContainingDeclarationBody(): BindingContext? {
    val enclosingDeclaration = getStrictParentOfType<JetDeclaration>()
    val bodyElement = when (enclosingDeclaration) {
        is JetDeclarationWithBody -> enclosingDeclaration.getBodyExpression()
        is JetWithExpressionInitializer -> enclosingDeclaration.getInitializer()
        is JetMultiDeclaration -> enclosingDeclaration.getInitializer()
        is JetParameter -> enclosingDeclaration.getDefaultValue()
        is JetClassInitializer -> enclosingDeclaration.getBody()
        is JetClass -> {
            val delegationSpecifierList = enclosingDeclaration.getDelegationSpecifierList()
            if (delegationSpecifierList.isAncestor(this)) this else null
        }
        else -> null
    }
    return bodyElement?.let { it.analyze() }
}

public fun chooseContainerElement<T>(
        containers: List<T>,
        editor: Editor,
        title: String,
        highlightSelection: Boolean,
        toPsi: (T) -> PsiElement,
        onSelect: (T) -> Unit) {
    return getPsiElementPopup(
            editor,
            containers,
            object : PsiElementListCellRenderer<PsiElement>() {
                private fun PsiElement.renderName(): String {
                    if (this is JetPropertyAccessor) {
                        return (getParent() as JetProperty).renderName() + if (isGetter()) ".get" else ".set"
                    }
                    if (this is JetObjectDeclaration && this.isCompanion()) {
                        return "Companion object of ${getStrictParentOfType<JetClassOrObject>()?.renderName() ?: "<anonymous>"}"
                    }
                    return (this as? PsiNamedElement)?.getName() ?: "<anonymous>"
                }

                private fun PsiElement.renderDeclaration(): String? {
                    val descriptor = when {
                        this is JetFile -> getName()
                        this is JetElement -> analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
                        this is PsiMember -> getJavaMemberDescriptor()
                        else -> null
                    } ?: return null
                    val name = renderName()
                    val params = (descriptor as? FunctionDescriptor)?.let { descriptor ->
                        descriptor.getValueParameters()
                                .map { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it.getType()) }
                                .joinToString(", ", "(", ")")
                    } ?: ""
                    return "$name$params"
                }

                private fun PsiElement.renderText(): String {
                    return StringUtil.shortenTextWithEllipsis(getText()!!.collapseSpaces(), 53, 0)
                }

                private fun PsiElement.getRepresentativeElement(): PsiElement {
                    return when (this) {
                        is JetBlockExpression -> (getParent() as? JetDeclarationWithBody) ?: this
                        is JetClassBody -> getParent() as JetClassOrObject
                        else -> this
                    }
                }

                override fun getElementText(element: PsiElement): String? {
                    val representativeElement = element.getRepresentativeElement()
                    return representativeElement.renderDeclaration() ?: representativeElement.renderText()
                }

                override fun getContainerText(element: PsiElement, name: String?): String? = null

                override fun getIconFlags(): Int = 0

                override fun getIcon(element: PsiElement): Icon? =
                        super.getIcon(element.getRepresentativeElement())
            },
            title,
            highlightSelection,
            toPsi,
            {
                onSelect(it)
                true
            }
    ).showInBestPositionFor(editor)
}

public fun chooseContainerElementIfNecessary<T>(
        containers: List<T>,
        editor: Editor,
        title: String,
        highlightSelection: Boolean,
        toPsi: (T) -> PsiElement,
        onSelect: (T) -> Unit
) {
    when {
        containers.isEmpty() -> return
        containers.size() == 1 || ApplicationManager.getApplication()!!.isUnitTestMode() -> onSelect(containers.first())
        else -> chooseContainerElement(containers, editor, title, highlightSelection, toPsi, onSelect)
    }
}

public fun PsiElement.isTrueJavaMethod(): Boolean = this is PsiMethod && this !is KotlinLightMethod

public fun PsiElement.canRefactor(): Boolean {
    return when {
        this is PsiPackage ->
            getDirectories().any { it.canRefactor() }
        this is JetElement,
        this is PsiMember && getLanguage() == JavaLanguage.INSTANCE,
        this is PsiDirectory ->
            isWritable() && ProjectRootsUtil.isInProjectSource(this)
        else ->
            false
    }
}

private fun copyModifierListItems(from: PsiModifierList, to: PsiModifierList, withPsiModifiers: Boolean = true) {
    if (withPsiModifiers) {
        for (modifier in PsiModifier.MODIFIERS) {
            if (from.hasExplicitModifier(modifier)) {
                to.setModifierProperty(modifier, true)
            }
        }
    }
    for (annotation in from.getAnnotations()) {
        val annotationName = annotation.getQualifiedName()!!
        if (KotlinBuiltIns.FQ_NAMES.annotation.asString() != annotationName
            && javaClass<Retention>().getName() != annotationName) {
            to.addAnnotation(annotationName)
        }
    }
}

private fun copyTypeParameters<T: PsiTypeParameterListOwner>(
        from: T,
        to: T,
        inserter: (T, PsiTypeParameterList) -> Unit
) where T : PsiNameIdentifierOwner {
    val factory = PsiElementFactory.SERVICE.getInstance((from : PsiElement).getProject())
    val templateTypeParams = from.getTypeParameterList()?.getTypeParameters() ?: PsiTypeParameter.EMPTY_ARRAY
    if (templateTypeParams.isNotEmpty()) {
        inserter(to, factory.createTypeParameterList())
        val targetTypeParamList = to.getTypeParameterList()
        val newTypeParams = templateTypeParams.map {
            factory.createTypeParameter(it.getName(), it.getExtendsList().getReferencedTypes())
        }
        ChangeSignatureUtil.synchronizeList(
                targetTypeParamList,
                newTypeParams,
                { it!!.getTypeParameters().toList() },
                BooleanArray(newTypeParams.size())
        )
    }
}

public fun createJavaMethod(function: JetFunction, targetClass: PsiClass): PsiMethod {
    val template = LightClassUtil.getLightClassMethod(function)
                   ?: throw AssertionError("Can't generate light method: ${function.getElementTextWithContext()}")
    return createJavaMethod(template, targetClass)
}

public fun createJavaMethod(template: PsiMethod, targetClass: PsiClass): PsiMethod {
    val factory = PsiElementFactory.SERVICE.getInstance(template.getProject())
    val methodToAdd = if (template.isConstructor()) {
        factory.createConstructor(template.getName())
    }
    else {
        factory.createMethod(template.getName(), template.getReturnType())
    }
    val method = targetClass.add(methodToAdd) as PsiMethod

    copyModifierListItems(template.getModifierList(), method.getModifierList())
    if (targetClass.isInterface()) {
        method.getModifierList().setModifierProperty(PsiModifier.FINAL, false)
    }

    copyTypeParameters(template, method) { method, typeParameterList ->
        method.addAfter(typeParameterList, method.getModifierList())
    }

    val targetParamList = method.getParameterList()
    val newParams = template.getParameterList().getParameters().map {
        val param = factory.createParameter(it.getName()!!, it.getType())
        copyModifierListItems(it.getModifierList()!!, param.getModifierList()!!)
        param
    }
    ChangeSignatureUtil.synchronizeList(
            targetParamList,
            newParams,
            { it.getParameters().toList() },
            BooleanArray(newParams.size())
    )

    if (template.getModifierList().hasModifierProperty(PsiModifier.ABSTRACT) || targetClass.isInterface()) {
        method.getBody()!!.delete()
    }
    else if (!template.isConstructor()) {
        CreateFromUsageUtils.setupMethodBody(method)
    }

    return method
}

fun createJavaField(property: JetProperty, targetClass: PsiClass): PsiField {
    val template = LightClassUtil.getLightClassPropertyMethods(property).getGetter()
                   ?: throw AssertionError("Can't generate light method: ${property.getElementTextWithContext()}")

    val factory = PsiElementFactory.SERVICE.getInstance(template.getProject())
    val field = targetClass.add(factory.createField(property.getName()!!, template.getReturnType()!!)) as PsiField

    with(field.getModifierList()!!) {
        val templateModifiers = template.getModifierList()
        setModifierProperty(VisibilityUtil.getVisibilityModifier(templateModifiers), true)
        if (!property.isVar() || targetClass.isInterface()) {
            setModifierProperty(PsiModifier.FINAL, true)
        }
        copyModifierListItems(templateModifiers, this, false)
    }

    return field
}

fun createJavaClass(klass: JetClass, targetClass: PsiClass?, forcePlainClass: Boolean = false): PsiClass {
    val kind = if (forcePlainClass) ClassKind.CLASS else (klass.resolveToDescriptor() as ClassDescriptor).getKind()

    val factory = PsiElementFactory.SERVICE.getInstance(klass.getProject())
    val className = klass.getName()!!
    val javaClassToAdd = when (kind) {
        ClassKind.CLASS -> factory.createClass(className)
        ClassKind.INTERFACE -> factory.createInterface(className)
        ClassKind.ANNOTATION_CLASS -> factory.createAnnotationType(className)
        ClassKind.ENUM_CLASS -> factory.createEnum(className)
        else -> throw AssertionError("Unexpected class kind: ${klass.getElementTextWithContext()}")
    }
    val javaClass = (targetClass?.add(javaClassToAdd) ?: javaClassToAdd) as PsiClass

    val template = LightClassUtil.getPsiClass(klass)
                   ?: throw AssertionError("Can't generate light class: ${klass.getElementTextWithContext()}")

    copyModifierListItems(template.getModifierList()!!, javaClass.getModifierList()!!)
    if (template.isInterface()) {
        javaClass.getModifierList()!!.setModifierProperty(PsiModifier.ABSTRACT, false)
    }

    copyTypeParameters(template, javaClass) { klass, typeParameterList ->
        klass.addAfter(typeParameterList, klass.getNameIdentifier())
    }

    // Turning interface to class
    if (!javaClass.isInterface && template.isInterface) {
        val implementsList = factory.createReferenceListWithRole(
                template.extendsList?.referenceElements ?: PsiJavaCodeReferenceElement.EMPTY_ARRAY,
                PsiReferenceList.Role.IMPLEMENTS_LIST
        )
        implementsList?.let { javaClass.implementsList?.replace(it) }
    }
    else {
        val extendsList = factory.createReferenceListWithRole(
                template.extendsList?.referenceElements ?: PsiJavaCodeReferenceElement.EMPTY_ARRAY,
                PsiReferenceList.Role.EXTENDS_LIST
        )
        extendsList?.let { javaClass.extendsList?.replace(it) }

        val implementsList = factory.createReferenceListWithRole(
                template.implementsList?.referenceElements ?: PsiJavaCodeReferenceElement.EMPTY_ARRAY,
                PsiReferenceList.Role.IMPLEMENTS_LIST
        )
        implementsList?.let { javaClass.implementsList?.replace(it) }
    }

    for (method in template.getMethods()) {
        val hasParams = method.getParameterList().getParametersCount() > 0
        val needSuperCall = !template.isEnum() &&
                            (template.getSuperClass()?.getConstructors() ?: PsiMethod.EMPTY_ARRAY).all {
                                it.getParameterList().getParametersCount() > 0
                            }
        if (method.isConstructor() && !(hasParams || needSuperCall)) continue
        with(createJavaMethod(method, javaClass)) {
            if (isConstructor() && needSuperCall) {
                getBody()!!.add(factory.createStatementFromText("super();", this))
            }
        }
    }

    return javaClass
}

fun PsiElement.j2kText(): String? {
    if (language != JavaLanguage.INSTANCE) return null

    val j2kConverter = JavaToKotlinConverter(project,
                                             ConverterSettings.defaultSettings,
                                             IdeaJavaToKotlinServices)
    return j2kConverter.elementsToKotlin(listOf(this)).results.single()?.text ?: return null //TODO: insert imports
}

fun PsiExpression.j2k(): JetExpression? {
    val text = j2kText() ?: return null
    return JetPsiFactory(project).createExpression(text)
}

fun PsiMember.j2k(): JetNamedDeclaration? {
    val text = j2kText() ?: return null
    return JetPsiFactory(project).createDeclaration(text)
}

public fun (() -> Any).runRefactoringWithPostprocessing(
        project: Project,
        targetRefactoringId: String,
        finishAction: () -> Unit
) {
    val connection = project.getMessageBus().connect()
    connection.subscribe(RefactoringEventListener.REFACTORING_EVENT_TOPIC,
                         object: RefactoringEventListener {
                             override fun undoRefactoring(refactoringId: String) {

                             }

                             override fun refactoringStarted(refactoringId: String, beforeData: RefactoringEventData?) {

                             }

                             override fun conflictsDetected(refactoringId: String, conflictsData: RefactoringEventData) {

                             }

                             override fun refactoringDone(refactoringId: String, afterData: RefactoringEventData?) {
                                 if (refactoringId == targetRefactoringId) {
                                     try {
                                         finishAction()
                                     }
                                     finally {
                                         connection.disconnect()
                                     }
                                 }
                             }
                         })
    this()
}

@throws(ConfigurationException::class)
public fun JetElement.validateElement(errorMessage: String) {
    try {
        AnalyzingUtils.checkForSyntacticErrors(this)
    }
    catch(e: Exception) {
        throw ConfigurationException(errorMessage)
    }
}