/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils
import com.intellij.codeInsight.unwrap.RangeSplitter
import com.intellij.codeInsight.unwrap.UnwrapHandler
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandAdapter
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupAdapter
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.util.Pass
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
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.intentions.RemoveCurlyBracesFromTemplateIntention
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.string.collapseSpaces
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.JavaToKotlinConverter
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCallWithAssert
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import java.io.File
import java.lang.annotation.Retention
import java.util.*
import javax.swing.Icon

@JvmOverloads
fun getOrCreateKotlinFile(fileName: String,
                          targetDir: PsiDirectory,
                          packageName: String? = targetDir.getPackage()?.qualifiedName): KtFile? =
        (targetDir.findFile(fileName) ?: createKotlinFile(fileName, targetDir, packageName)) as? KtFile

fun createKotlinFile(fileName: String,
                     targetDir: PsiDirectory,
                     packageName: String? = targetDir.getPackage()?.qualifiedName): KtFile {
    targetDir.checkCreateFile(fileName)
    val file = PsiFileFactory.getInstance(targetDir.project).createFileFromText(
            fileName, KotlinFileType.INSTANCE, if (!packageName.isNullOrBlank()) "package $packageName \n\n" else ""
    )

    return targetDir.add(file) as KtFile
}

fun File.toVirtualFile(): VirtualFile? = LocalFileSystem.getInstance().findFileByIoFile(this)

fun File.toPsiFile(project: Project): PsiFile? = toVirtualFile()?.toPsiFile(project)

fun File.toPsiDirectory(project: Project): PsiDirectory? {
    return toVirtualFile()?.let { vfile -> PsiManager.getInstance(project).findDirectory(vfile) }
}

fun VirtualFile.toPsiFile(project: Project): PsiFile? = PsiManager.getInstance(project).findFile(this)

fun VirtualFile.toPsiDirectory(project: Project): PsiDirectory? = PsiManager.getInstance(project).findDirectory(this)

fun PsiElement.getUsageContext(): PsiElement {
    return when (this) {
        is KtElement -> PsiTreeUtil.getParentOfType(this, KtNamedDeclaration::class.java, KtFile::class.java)!!
        else -> ConflictsUtil.getContainer(this)
    }
}

fun PsiElement.isInJavaSourceRoot(): Boolean =
        !JavaProjectRootsUtil.isOutsideJavaSourceRoot(containingFile)

fun KtFile.createTempCopy(text: String? = null): KtFile {
    val tmpFile = KtPsiFactory(this).createAnalyzableFile(name, text ?: this.text ?: "", this)
    tmpFile.originalFile = this
    tmpFile.suppressDiagnosticsInDebugMode = suppressDiagnosticsInDebugMode
    return tmpFile
}

fun PsiElement.getAllExtractionContainers(strict: Boolean = true): List<KtElement> {
    val containers = ArrayList<KtElement>()

    var objectOrNonInnerNestedClassFound = false
    val parents = if (strict) parents else parentsWithSelf
    for (element in parents) {
        val isValidContainer = when (element) {
            is KtFile -> true
            is KtClassBody -> !objectOrNonInnerNestedClassFound || element.parent is KtObjectDeclaration
            is KtBlockExpression -> !objectOrNonInnerNestedClassFound
            else -> false
        }
        if (!isValidContainer) continue

        containers.add(element as KtElement)

        if (!objectOrNonInnerNestedClassFound) {
            val bodyParent = (element as? KtClassBody)?.parent
            objectOrNonInnerNestedClassFound =
                    (bodyParent is KtObjectDeclaration && !bodyParent.isObjectLiteral())
                    || (bodyParent is KtClass && !bodyParent.isInner())
        }
    }

    return containers
}

fun PsiElement.getExtractionContainers(strict: Boolean = true, includeAll: Boolean = false): List<KtElement> {
    fun getEnclosingDeclaration(element: PsiElement, strict: Boolean): PsiElement? {
        return (if (strict) element.parents else element.parentsWithSelf)
                .filter {
                    (it is KtDeclarationWithBody && it !is KtFunctionLiteral)
                    || it is KtAnonymousInitializer
                    || it is KtClassBody
                    || it is KtFile
                }
                .firstOrNull()
    }

    if (includeAll) return getAllExtractionContainers(strict)

    val enclosingDeclaration = getEnclosingDeclaration(this, strict)?.let {
        if (it is KtDeclarationWithBody || it is KtAnonymousInitializer) getEnclosingDeclaration(it, true) else it
    }

    return when (enclosingDeclaration) {
        is KtFile -> Collections.singletonList(enclosingDeclaration)
        is KtClassBody -> getAllExtractionContainers(strict).filterIsInstance<KtClassBody>()
        else -> {
            val targetContainer = when (enclosingDeclaration) {
                is KtDeclarationWithBody -> enclosingDeclaration.bodyExpression
                is KtAnonymousInitializer -> enclosingDeclaration.body
                else -> null
            }
            if (targetContainer is KtBlockExpression) Collections.singletonList(targetContainer) else Collections.emptyList()
        }
    }
}

fun Project.checkConflictsInteractively(
        conflicts: MultiMap<PsiElement, String>,
        onShowConflicts: () -> Unit = {},
        onAccept: () -> Unit) {
    if (!conflicts.isEmpty) {
        if (ApplicationManager.getApplication()!!.isUnitTestMode) throw ConflictsInTestsException(conflicts.values())

        val dialog = ConflictsDialog(this, conflicts) { onAccept() }
        dialog.show()
        if (!dialog.isOK) {
            if (dialog.isShowConflicts) {
                onShowConflicts()
            }
            return
        }
    }

    onAccept()
}

fun reportDeclarationConflict(
        conflicts: MultiMap<PsiElement, String>,
        declaration: PsiElement,
        message: (renderedDeclaration: String) -> String
) {
    conflicts.putValue(declaration, message(RefactoringUIUtil.getDescription(declaration, true).capitalize()))
}

fun <T, E: PsiElement> getPsiElementPopup(
        editor: Editor,
        elements: List<T>,
        renderer: PsiElementListCellRenderer<E>,
        title: String?,
        highlightSelection: Boolean,
        toPsi: (T) -> E,
        processor: (T) -> Boolean): JBPopup {
    val highlighter = if (highlightSelection) SelectionAwareScopeHighlighter(editor) else null

    val list = JBList(elements.map(toPsi))
    list.cellRenderer = renderer
    list.addListSelectionListener { e ->
        highlighter?.dropHighlight()
        val index = list.selectedIndex
        if (index >= 0) {
            highlighter?.highlight(list.model!!.getElementAt(index) as PsiElement)
        }
    }

    return with(PopupChooserBuilder(list)) {
        title?.let { setTitle(it) }
        renderer.installSpeedSearch(this, true)
        setItemChoosenCallback {
            val index = list.selectedIndex
            if (index >= 0) {
                processor(elements[index])
            }
        }
        addListener(object: JBPopupAdapter() {
            override fun onClosed(event: LightweightWindowEvent?) {
                highlighter?.dropHighlight()
            }
        })

        createPopup()
    }
}

class SelectionAwareScopeHighlighter(val editor: Editor) {
    private val highlighters = ArrayList<RangeHighlighter>()

    private fun addHighlighter(r: TextRange, attr: TextAttributes) {
        highlighters.add(
                editor.markupModel.addRangeHighlighter(
                        r.startOffset,
                        r.endOffset,
                        UnwrapHandler.HIGHLIGHTER_LEVEL,
                        attr,
                        HighlighterTargetArea.EXACT_RANGE
                )
        )
    }

    fun highlight(wholeAffected: PsiElement) {
        dropHighlight()

        val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)!!
        val selectedRange = with(editor.selectionModel) { TextRange(selectionStart, selectionEnd) }
        for (r in RangeSplitter.split(wholeAffected.textRange!!, Collections.singletonList(selectedRange))) {
            addHighlighter(r, attributes)
        }
    }

    fun dropHighlight() {
        highlighters.forEach { it.dispose() }
        highlighters.clear()
    }
}

fun PsiFile.getLineStartOffset(line: Int): Int? {
    val doc = PsiDocumentManager.getInstance(project).getDocument(this)
    if (doc != null && line >= 0 && line < doc.lineCount) {
        val startOffset = doc.getLineStartOffset(line)
        val element = findElementAt(startOffset) ?: return startOffset

        return PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace::class.java, PsiComment::class.java)?.startOffset ?: startOffset
    }

    return null
}

fun PsiFile.getLineEndOffset(line: Int): Int? {
    return PsiDocumentManager.getInstance(project).getDocument(this)?.getLineEndOffset(line)
}

fun PsiElement.getLineNumber(start: Boolean = true): Int {
    return PsiDocumentManager.getInstance(project).getDocument(this.containingFile)?.getLineNumber(if (start) this.startOffset else this.endOffset) ?: 0
}

fun PsiElement.getLineCount(): Int {
    val doc = containingFile?.let { file -> PsiDocumentManager.getInstance(project).getDocument(file) }
    if (doc != null) {
        val spaceRange = textRange ?: TextRange.EMPTY_RANGE

        val startLine = doc.getLineNumber(spaceRange.startOffset)
        val endLine = doc.getLineNumber(spaceRange.endOffset)

        return endLine - startLine
    }

    return (text ?: "").count { it == '\n' } + 1
}

fun PsiElement.isMultiLine(): Boolean = getLineCount() > 1

fun <T> chooseContainerElement(
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
                    if (this is KtPropertyAccessor) {
                        return property.renderName() + if (isGetter) ".get" else ".set"
                    }
                    if (this is KtObjectDeclaration && this.isCompanion()) {
                        return "Companion object of ${getStrictParentOfType<KtClassOrObject>()?.renderName() ?: "<anonymous>"}"
                    }
                    return (this as? PsiNamedElement)?.name ?: "<anonymous>"
                }

                private fun PsiElement.renderDeclaration(): String? {
                    if (this is KtFunctionLiteral || isFunctionalExpression()) return renderText()

                    val descriptor = when {
                                         this is KtFile -> name
                                         this is KtElement -> analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
                                         this is PsiMember -> getJavaMemberDescriptor()
                                         else -> null
                                     } ?: return null
                    val name = renderName()
                    val params = (descriptor as? FunctionDescriptor)?.let { descriptor ->
                        descriptor.valueParameters
                                .map { DescriptorRenderer.Companion.SHORT_NAMES_IN_TYPES.renderType(it.type) }
                                .joinToString(", ", "(", ")")
                    } ?: ""
                    return "$name$params"
                }

                private fun PsiElement.renderText(): String {
                    return StringUtil.shortenTextWithEllipsis(text!!.collapseSpaces(), 53, 0)
                }

                private fun PsiElement.getRepresentativeElement(): PsiElement {
                    return when (this) {
                        is KtBlockExpression -> (parent as? KtDeclarationWithBody) ?: this
                        is KtClassBody -> parent as KtClassOrObject
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

fun <T> chooseContainerElementIfNecessary(
        containers: List<T>,
        editor: Editor,
        title: String,
        highlightSelection: Boolean,
        toPsi: (T) -> PsiElement,
        onSelect: (T) -> Unit
) {
    when {
        containers.isEmpty() -> return
        containers.size == 1 || ApplicationManager.getApplication()!!.isUnitTestMode -> onSelect(containers.first())
        else -> chooseContainerElement(containers, editor, title, highlightSelection, toPsi, onSelect)
    }
}

fun PsiElement.isTrueJavaMethod(): Boolean = this is PsiMethod && this !is KtLightMethod

fun PsiElement.canRefactor(): Boolean {
    if (!this.isValid) return false

    return when {
        this is PsiPackage ->
            directories.any { it.canRefactor() }
        this is KtElement ||
        this is PsiMember && language == JavaLanguage.INSTANCE ||
        this is PsiDirectory ->
            isWritable && ProjectRootsUtil.isInProjectSource(this)
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
    for (annotation in from.annotations) {
        val annotationName = annotation.qualifiedName!!

        if (Retention::class.java.name != annotationName) {
            to.addAnnotation(annotationName)
        }
    }
}

private fun <T> copyTypeParameters(
        from: T,
        to: T,
        inserter: (T, PsiTypeParameterList) -> Unit
) where T : PsiTypeParameterListOwner, T : PsiNameIdentifierOwner {
    val factory = PsiElementFactory.SERVICE.getInstance((from as PsiElement).project)
    val templateTypeParams = from.typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY
    if (templateTypeParams.isNotEmpty()) {
        inserter(to, factory.createTypeParameterList())
        val targetTypeParamList = to.typeParameterList
        val newTypeParams = templateTypeParams.map {
            factory.createTypeParameter(it.name, it.extendsList.referencedTypes)
        }
        ChangeSignatureUtil.synchronizeList(
                targetTypeParamList,
                newTypeParams,
                { it!!.typeParameters.toList() },
                BooleanArray(newTypeParams.size)
        )
    }
}

fun createJavaMethod(function: KtFunction, targetClass: PsiClass): PsiMethod {
    val template = LightClassUtil.getLightClassMethod(function)
                   ?: throw AssertionError("Can't generate light method: ${function.getElementTextWithContext()}")
    return createJavaMethod(template, targetClass)
}

fun createJavaMethod(template: PsiMethod, targetClass: PsiClass): PsiMethod {
    val factory = PsiElementFactory.SERVICE.getInstance(template.project)
    val methodToAdd = if (template.isConstructor) {
        factory.createConstructor(template.name)
    }
    else {
        factory.createMethod(template.name, template.returnType)
    }
    val method = targetClass.add(methodToAdd) as PsiMethod

    copyModifierListItems(template.modifierList, method.modifierList)
    if (targetClass.isInterface) {
        method.modifierList.setModifierProperty(PsiModifier.FINAL, false)
    }

    copyTypeParameters(template, method) { method, typeParameterList ->
        method.addAfter(typeParameterList, method.modifierList)
    }

    val targetParamList = method.parameterList
    val newParams = template.parameterList.parameters.map {
        val param = factory.createParameter(it.name!!, it.type)
        copyModifierListItems(it.modifierList!!, param.modifierList!!)
        param
    }
    ChangeSignatureUtil.synchronizeList(
            targetParamList,
            newParams,
            { it.parameters.toList() },
            BooleanArray(newParams.size)
    )

    if (template.modifierList.hasModifierProperty(PsiModifier.ABSTRACT) || targetClass.isInterface) {
        method.body!!.delete()
    }
    else if (!template.isConstructor) {
        CreateFromUsageUtils.setupMethodBody(method)
    }

    return method
}

fun createJavaField(property: KtProperty, targetClass: PsiClass): PsiField {
    val template = LightClassUtil.getLightClassPropertyMethods(property).getter
                   ?: throw AssertionError("Can't generate light method: ${property.getElementTextWithContext()}")

    val factory = PsiElementFactory.SERVICE.getInstance(template.project)
    val field = targetClass.add(factory.createField(property.name!!, template.returnType!!)) as PsiField

    with(field.modifierList!!) {
        val templateModifiers = template.modifierList
        setModifierProperty(VisibilityUtil.getVisibilityModifier(templateModifiers), true)
        if (!property.isVar || targetClass.isInterface) {
            setModifierProperty(PsiModifier.FINAL, true)
        }
        copyModifierListItems(templateModifiers, this, false)
    }

    return field
}

fun createJavaClass(klass: KtClass, targetClass: PsiClass?, forcePlainClass: Boolean = false): PsiClass {
    val kind = if (forcePlainClass) ClassKind.CLASS else (klass.resolveToDescriptor() as ClassDescriptor).kind

    val factory = PsiElementFactory.SERVICE.getInstance(klass.project)
    val className = klass.name!!
    val javaClassToAdd = when (kind) {
        ClassKind.CLASS -> factory.createClass(className)
        ClassKind.INTERFACE -> factory.createInterface(className)
        ClassKind.ANNOTATION_CLASS -> factory.createAnnotationType(className)
        ClassKind.ENUM_CLASS -> factory.createEnum(className)
        else -> throw AssertionError("Unexpected class kind: ${klass.getElementTextWithContext()}")
    }
    val javaClass = (targetClass?.add(javaClassToAdd) ?: javaClassToAdd) as PsiClass

    val template = klass.toLightClass() ?: throw AssertionError("Can't generate light class: ${klass.getElementTextWithContext()}")

    copyModifierListItems(template.modifierList!!, javaClass.modifierList!!)
    if (template.isInterface) {
        javaClass.modifierList!!.setModifierProperty(PsiModifier.ABSTRACT, false)
    }

    copyTypeParameters(template, javaClass) { klass, typeParameterList ->
        klass.addAfter(typeParameterList, klass.nameIdentifier)
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

    for (method in template.methods) {
        val hasParams = method.parameterList.parametersCount > 0
        val needSuperCall = !template.isEnum &&
                            (template.superClass?.constructors ?: PsiMethod.EMPTY_ARRAY).all {
                                it.parameterList.parametersCount > 0
                            }
        if (method.isConstructor && !(hasParams || needSuperCall)) continue
        with(createJavaMethod(method, javaClass)) {
            if (isConstructor && needSuperCall) {
                body!!.add(factory.createStatementFromText("super();", this))
            }
        }
    }

    return javaClass
}

fun PsiElement.j2kText(): String? {
    if (language != JavaLanguage.INSTANCE) return null

    val j2kConverter = JavaToKotlinConverter(project,
                                             ConverterSettings.Companion.defaultSettings,
                                             IdeaJavaToKotlinServices)
    return j2kConverter.elementsToKotlin(listOf(this)).results.single()?.text ?: return null //TODO: insert imports
}

fun PsiExpression.j2k(): KtExpression? {
    val text = j2kText() ?: return null
    return KtPsiFactory(project).createExpression(text)
}

fun PsiMember.j2k(): KtNamedDeclaration? {
    val text = j2kText() ?: return null
    return KtPsiFactory(project).createDeclaration(text)
}

fun (() -> Any).runRefactoringWithPostprocessing(
        project: Project,
        targetRefactoringId: String,
        finishAction: () -> Unit
) {
    val connection = project.messageBus.connect()
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

@Throws(ConfigurationException::class) fun KtElement?.validateElement(errorMessage: String) {
    if (this == null) throw ConfigurationException(errorMessage)

    try {
        AnalyzingUtils.checkForSyntacticErrors(this)
    }
    catch(e: Exception) {
        throw ConfigurationException(errorMessage)
    }
}

fun <T : Any> Project.runSynchronouslyWithProgress(progressTitle: String, canBeCanceled: Boolean, action: () -> T): T? {
    var result: T? = null
    ProgressManager.getInstance().runProcessWithProgressSynchronously({ result = action() }, progressTitle, canBeCanceled, this)
    return result
}

fun invokeOnceOnCommandFinish(action: () -> Unit) {
    val commandProcessor = CommandProcessor.getInstance()
    val listener = object: CommandAdapter() {
        override fun beforeCommandFinished(event: CommandEvent?) {
            action()
            commandProcessor.removeCommandListener(this)
        }
    }
    commandProcessor.addCommandListener(listener)
}

fun FqNameUnsafe.hasIdentifiersOnly(): Boolean = pathSegments().all { KotlinNameSuggester.isIdentifier(it.asString().quoteIfNeeded()) }

fun FqName.hasIdentifiersOnly(): Boolean = pathSegments().all { KotlinNameSuggester.isIdentifier(it.asString().quoteIfNeeded()) }

fun PsiNamedElement.isInterfaceClass(): Boolean = this is KtClass && isInterface() || this is PsiClass && isInterface

fun <ListType : KtElement> replaceListPsiAndKeepDelimiters(
        originalList: ListType,
        newList: ListType,
        @Suppress("UNCHECKED_CAST") listReplacer: ListType.(ListType) -> ListType = { replace(it) as ListType },
        itemsFun: ListType.() -> List<KtElement>
): ListType {
    originalList.children.takeWhile { it is PsiErrorElement }.forEach { it.delete() }

    val oldParameters = originalList.itemsFun().toMutableList()
    val newParameters = newList.itemsFun()
    val oldCount = oldParameters.size
    val newCount = newParameters.size

    val commonCount = Math.min(oldCount, newCount)
    for (i in 0..commonCount - 1) {
        oldParameters[i] = oldParameters[i].replace(newParameters[i]) as KtElement
    }

    if (commonCount == 0) return originalList.listReplacer(newList)

    if (oldCount > commonCount) {
        originalList.deleteChildRange(oldParameters[commonCount - 1].nextSibling, oldParameters.last())
    }
    else if (newCount > commonCount) {
        originalList.addRangeAfter(newParameters[commonCount - 1].nextSibling, newParameters.last(), oldParameters.last())
    }

    return originalList
}

fun <T> Pass(body: (T) -> Unit) = object: Pass<T>() {
    override fun pass(t: T) = body(t)
}

fun KtExpression.removeTemplateEntryBracesIfPossible(): KtExpression {
    val parent = parent
    if (parent !is KtBlockStringTemplateEntry) return this

    val intention = RemoveCurlyBracesFromTemplateIntention()
    val newEntry = if (intention.isApplicableTo(parent)) intention.applyTo(parent) else parent
    return newEntry.expression!!
}

fun dropOverrideKeywordIfNecessary(element: KtNamedDeclaration) {
    val callableDescriptor = element.resolveToDescriptor() as? CallableDescriptor ?: return
    if (callableDescriptor.overriddenDescriptors.isEmpty()) {
        element.removeModifier(KtTokens.OVERRIDE_KEYWORD)
    }
}

fun getQualifiedTypeArgumentList(initializer: KtExpression): KtTypeArgumentList? {
    val context = initializer.analyze(BodyResolveMode.PARTIAL)
    val call = initializer.getResolvedCall(context) ?: return null
    val typeArgumentMap = call.typeArguments
    val typeArguments = call.candidateDescriptor.typeParameters.mapNotNull { typeArgumentMap[it] }
    val renderedList = typeArguments.joinToString(prefix = "<", postfix = ">") {
        IdeDescriptorRenderers.SOURCE_CODE_NOT_NULL_TYPE_APPROXIMATION.renderType(it)
    }
    return KtPsiFactory(initializer).createTypeArguments(renderedList)
}

fun addTypeArgumentsIfNeeded(expression: KtExpression, typeArgumentList: KtTypeArgumentList) {
    val context = expression.analyze(BodyResolveMode.PARTIAL)
    val call = expression.getCallWithAssert(context)
    val callElement = call.callElement as? KtCallExpression ?: return
    if (call.typeArgumentList != null) return
    val callee = call.calleeExpression ?: return
    if (context.diagnostics.forElement(callee).all { it.factory != Errors.TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER }) return

    callElement.addAfter(typeArgumentList, callElement.calleeExpression)
    ShortenReferences.DEFAULT.process(callElement.typeArgumentList!!)
}

internal fun DeclarationDescriptor.getThisLabelName(): String {
    if (!name.isSpecial) return name.asString()
    if (this is AnonymousFunctionDescriptor) {
        val function = source.getPsi() as? KtFunction
        val argument = function?.parent as? KtValueArgument
        val callElement = argument?.getStrictParentOfType<KtCallElement>()
        val callee = callElement?.calleeExpression as? KtSimpleNameExpression
        if (callee != null) return callee.text
    }
    return ""
}

internal fun DeclarationDescriptor.explicateAsTextForReceiver(): String {
    val labelName = getThisLabelName()
    return if (labelName.isEmpty()) "this" else "this@$labelName"
}

internal fun ImplicitReceiver.explicateAsText(): String {
    return declarationDescriptor.explicateAsTextForReceiver()
}

val PsiFile.isInjectedFragment: Boolean
    get() = InjectedLanguageManager.getInstance(project).isInjectedFragment(this)

val PsiElement.isInsideInjectedFragment: Boolean
    get() = containingFile.isInjectedFragment
