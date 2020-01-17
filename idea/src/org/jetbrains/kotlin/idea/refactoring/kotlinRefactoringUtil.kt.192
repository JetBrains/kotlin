/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils
import com.intellij.codeInsight.unwrap.RangeSplitter
import com.intellij.codeInsight.unwrap.UnwrapHandler
import com.intellij.ide.IdeBundle
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.util.Pass
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.file.PsiPackageBase
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.refactoring.changeSignature.ChangeSignatureUtil
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.ui.ConflictsDialog
import com.intellij.refactoring.util.ConflictsUtil
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.ui.components.JBList
import com.intellij.usageView.UsageViewTypeLocation
import com.intellij.util.VisibilityUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.getAccessorLightMethods
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.core.util.showYesNoCancelDialog
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinValVar
import org.jetbrains.kotlin.idea.refactoring.changeSignature.toValVar
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KtPsiClassWrapper
import org.jetbrains.kotlin.idea.refactoring.rename.canonicalRender
import org.jetbrains.kotlin.idea.roots.isOutsideKotlinAwareSourceRoot
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.idea.util.liftToExpected
import org.jetbrains.kotlin.idea.util.string.collapseSpaces
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getCallWithAssert
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import java.lang.annotation.Retention
import java.util.*
import javax.swing.Icon
import kotlin.math.min

import org.jetbrains.kotlin.idea.core.util.getLineCount as newGetLineCount
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory as newToPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile as newToPsiFile

const val CHECK_SUPER_METHODS_YES_NO_DIALOG = "CHECK_SUPER_METHODS_YES_NO_DIALOG"

@JvmOverloads
fun getOrCreateKotlinFile(
    fileName: String,
    targetDir: PsiDirectory,
    packageName: String? = targetDir.getFqNameWithImplicitPrefix()?.asString()
): KtFile? =
    (targetDir.findFile(fileName) ?: createKotlinFile(fileName, targetDir, packageName)) as? KtFile

fun createKotlinFile(
    fileName: String,
    targetDir: PsiDirectory,
    packageName: String? = targetDir.getFqNameWithImplicitPrefix()?.asString()
): KtFile {
    targetDir.checkCreateFile(fileName)
    val packageFqName = packageName?.let(::FqName) ?: FqName.ROOT
    val file = PsiFileFactory.getInstance(targetDir.project).createFileFromText(
        fileName, KotlinFileType.INSTANCE, if (!packageFqName.isRoot) "package ${packageFqName.quoteSegmentsIfNeeded()} \n\n" else ""
    )

    return targetDir.add(file) as KtFile
}

fun PsiElement.getUsageContext(): PsiElement {
    return when (this) {
        is KtElement -> PsiTreeUtil.getParentOfType(
            this,
            KtPropertyAccessor::class.java,
            KtProperty::class.java,
            KtNamedFunction::class.java,
            KtConstructor::class.java,
            KtClassOrObject::class.java
        ) ?: containingFile
        else -> ConflictsUtil.getContainer(this)
    }
}

fun PsiElement.isInKotlinAwareSourceRoot(): Boolean =
    !isOutsideKotlinAwareSourceRoot(containingFile)

fun KtFile.createTempCopy(text: String? = null): KtFile {
    val tmpFile = KtPsiFactory(this).createAnalyzableFile(name, text ?: this.text ?: "", this)
    tmpFile.originalFile = this
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
                (it is KtDeclarationWithBody && it !is KtFunctionLiteral && !(it is KtNamedFunction && it.name == null))
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
    onAccept: () -> Unit
) {
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

fun <T, E : PsiElement> getPsiElementPopup(
    editor: Editor,
    elements: List<T>,
    renderer: PsiElementListCellRenderer<E>,
    title: String?,
    highlightSelection: Boolean,
    toPsi: (T) -> E,
    processor: (T) -> Boolean
): JBPopup {
    val highlighter = if (highlightSelection) SelectionAwareScopeHighlighter(editor) else null

    val list = JBList(elements.map(toPsi))
    list.cellRenderer = renderer
    list.addListSelectionListener {
        highlighter?.dropHighlight()
        val index = list.selectedIndex
        if (index >= 0) {
            highlighter?.highlight(list.model!!.getElementAt(index) as PsiElement)
        }
    }

    return with(PopupChooserBuilder<E>(list)) {
        title?.let { setTitle(it) }
        renderer.installSpeedSearch(this, true)
        setItemChoosenCallback {
            val index = list.selectedIndex
            if (index >= 0) {
                processor(elements[index])
            }
        }
        addListener(object : JBPopupAdapter() {
            override fun onClosed(event: LightweightWindowEvent) {
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

        val affectedRange = wholeAffected.textRange ?: return

        val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)!!
        val selectedRange = with(editor.selectionModel) { TextRange(selectionStart, selectionEnd) }
        val textLength = editor.document.textLength
        for (r in RangeSplitter.split(affectedRange, Collections.singletonList(selectedRange))) {
            if (r.endOffset <= textLength) addHighlighter(r, attributes)
        }
    }

    fun dropHighlight() {
        highlighters.forEach { it.dispose() }
        highlighters.clear()
    }
}

@Deprecated(
    "Use org.jetbrains.kotlin.idea.core.util.getLineStartOffset() instead",
    ReplaceWith("this.getLineStartOffset(line)", "org.jetbrains.kotlin.idea.core.util.getLineStartOffset"),
    DeprecationLevel.ERROR
)
fun PsiFile.getLineStartOffset(line: Int): Int? {
    val doc = viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(this)
    if (doc != null && line >= 0 && line < doc.lineCount) {
        val startOffset = doc.getLineStartOffset(line)
        val element = findElementAt(startOffset) ?: return startOffset

        if (element is PsiWhiteSpace || element is PsiComment) {
            return PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace::class.java, PsiComment::class.java)?.startOffset ?: startOffset
        }
        return startOffset
    }

    return null
}

@Deprecated(
    "Use org.jetbrains.kotlin.idea.core.util.getLineEndOffset() instead",
    ReplaceWith("this.getLineEndOffset(line)", "org.jetbrains.kotlin.idea.core.util.getLineEndOffset"),
    DeprecationLevel.ERROR
)
fun PsiFile.getLineEndOffset(line: Int): Int? {
    val document = viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(this)
    return document?.getLineEndOffset(line)
}

fun PsiElement.getLineNumber(start: Boolean = true): Int {
    val document = containingFile.viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(containingFile)
    val index = if (start) this.startOffset else this.endOffset
    if (index > document?.textLength ?: 0) return 0
    return document?.getLineNumber(index) ?: 0
}

class SeparateFileWrapper(manager: PsiManager) : LightElement(manager, KotlinLanguage.INSTANCE) {
    override fun toString() = ""
}

fun <T> chooseContainerElement(
    containers: List<T>,
    editor: Editor,
    title: String,
    highlightSelection: Boolean,
    toPsi: (T) -> PsiElement,
    onSelect: (T) -> Unit
) {
    val popup = getPsiElementPopup(
        editor,
        containers,
        object : PsiElementListCellRenderer<PsiElement>() {
            private fun PsiElement.renderName(): String = when {
                this is KtPropertyAccessor -> property.renderName() + if (isGetter) ".get" else ".set"
                this is KtObjectDeclaration && isCompanion() -> {
                    val name = getStrictParentOfType<KtClassOrObject>()?.renderName() ?: "<anonymous>"
                    "Companion object of $name"
                }
                else -> (this as? PsiNamedElement)?.name ?: "<anonymous>"
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
                val params = (descriptor as? FunctionDescriptor)?.valueParameters?.joinToString(
                    ", ",
                    "(",
                    ")"
                ) { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it.type) } ?: ""
                return "$name$params"
            }

            private fun PsiElement.renderText(): String = when (this) {
                is SeparateFileWrapper -> "Extract to separate file"
                is PsiPackageBase -> qualifiedName
                else -> {
                    val text = text ?: "<invalid text>"
                    StringUtil.shortenTextWithEllipsis(text.collapseSpaces(), 53, 0)
                }
            }

            private fun PsiElement.getRepresentativeElement(): PsiElement = when (this) {
                is KtBlockExpression -> (parent as? KtDeclarationWithBody) ?: this
                is KtClassBody -> parent as KtClassOrObject
                else -> this
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
    )
    ApplicationManager.getApplication().invokeLater {
        popup.showInBestPositionFor(editor)
    }
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

fun PsiElement.canRefactor(): Boolean = when {
    !isValid -> false
    this is PsiPackage -> directories.any { it.canRefactor() }
    this is KtElement || this is PsiMember && language == JavaLanguage.INSTANCE || this is PsiDirectory -> ProjectRootsUtil.isInProjectSource(
        this,
        includeScriptsOutsideSourceRoots = true
    )
    else -> false
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
        val annotationName = annotation.qualifiedName ?: continue

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
    } else {
        factory.createMethod(template.name, template.returnType)
    }
    val method = targetClass.add(methodToAdd) as PsiMethod

    copyModifierListItems(template.modifierList, method.modifierList)
    if (targetClass.isInterface) {
        method.modifierList.setModifierProperty(PsiModifier.FINAL, false)
    }

    copyTypeParameters(template, method) { psiMethod, typeParameterList ->
        psiMethod.addAfter(typeParameterList, psiMethod.modifierList)
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
    } else if (!template.isConstructor) {
        CreateFromUsageUtils.setupMethodBody(method)
    }

    return method
}

fun createJavaField(property: KtNamedDeclaration, targetClass: PsiClass): PsiField {
    val accessorLightMethods = property.getAccessorLightMethods()
    val template = accessorLightMethods.getter
        ?: throw AssertionError("Can't generate light method: ${property.getElementTextWithContext()}")

    val factory = PsiElementFactory.SERVICE.getInstance(template.project)
    val field = targetClass.add(factory.createField(property.name!!, template.returnType!!)) as PsiField

    with(field.modifierList!!) {
        val templateModifiers = template.modifierList
        setModifierProperty(VisibilityUtil.getVisibilityModifier(templateModifiers), true)
        if ((property as KtValVarKeywordOwner).valOrVarKeyword.toValVar() != KotlinValVar.Var || targetClass.isInterface) {
            setModifierProperty(PsiModifier.FINAL, true)
        }
        copyModifierListItems(templateModifiers, this, false)
    }

    return field
}

fun createJavaClass(klass: KtClass, targetClass: PsiClass?, forcePlainClass: Boolean = false): PsiClass {
    val kind = if (forcePlainClass) ClassKind.CLASS else (klass.unsafeResolveToDescriptor() as ClassDescriptor).kind

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

    copyTypeParameters(template, javaClass) { clazz, typeParameterList ->
        clazz.addAfter(typeParameterList, clazz.nameIdentifier)
    }

    // Turning interface to class
    if (!javaClass.isInterface && template.isInterface) {
        val implementsList = factory.createReferenceListWithRole(
            template.extendsList?.referenceElements ?: PsiJavaCodeReferenceElement.EMPTY_ARRAY,
            PsiReferenceList.Role.IMPLEMENTS_LIST
        )
        implementsList?.let { javaClass.implementsList?.replace(it) }
    } else {
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

internal fun broadcastRefactoringExit(project: Project, refactoringId: String) {
    project.messageBus.syncPublisher(KotlinRefactoringEventListener.EVENT_TOPIC).onRefactoringExit(refactoringId)
}

// IMPORTANT: Target refactoring must support KotlinRefactoringEventListener
internal abstract class CompositeRefactoringRunner(
    val project: Project,
    val refactoringId: String
) {
    protected abstract fun runRefactoring()

    protected open fun onRefactoringDone() {}
    protected open fun onExit() {}

    fun run() {
        val connection = project.messageBus.connect()
        connection.subscribe(
            RefactoringEventListener.REFACTORING_EVENT_TOPIC,
            object : RefactoringEventListener {
                override fun undoRefactoring(refactoringId: String) {

                }

                override fun refactoringStarted(refactoringId: String, beforeData: RefactoringEventData?) {

                }

                override fun conflictsDetected(refactoringId: String, conflictsData: RefactoringEventData) {

                }

                override fun refactoringDone(refactoringId: String, afterData: RefactoringEventData?) {
                    if (refactoringId == this@CompositeRefactoringRunner.refactoringId) {
                        onRefactoringDone()
                    }
                }
            }
        )
        connection.subscribe(
            KotlinRefactoringEventListener.EVENT_TOPIC,
            object : KotlinRefactoringEventListener {
                override fun onRefactoringExit(refactoringId: String) {
                    if (refactoringId == this@CompositeRefactoringRunner.refactoringId) {
                        try {
                            onExit()
                        } finally {
                            connection.disconnect()
                        }
                    }
                }
            }
        )
        runRefactoring()
    }
}

@Throws(ConfigurationException::class)
fun KtElement?.validateElement(errorMessage: String) {
    if (this == null) throw ConfigurationException(errorMessage)

    try {
        AnalyzingUtils.checkForSyntacticErrors(this)
    } catch (e: Exception) {
        throw ConfigurationException(errorMessage)
    }
}

fun invokeOnceOnCommandFinish(action: () -> Unit) {
    val commandProcessor = CommandProcessor.getInstance()
    val listener = object : CommandAdapter() {
        override fun beforeCommandFinished(event: CommandEvent) {
            action()
            commandProcessor.removeCommandListener(this)
        }
    }
    commandProcessor.addCommandListener(listener)
}

fun FqNameUnsafe.hasIdentifiersOnly(): Boolean = pathSegments().all { it.asString().quoteIfNeeded().isIdentifier() }

fun FqName.hasIdentifiersOnly(): Boolean = pathSegments().all { it.asString().quoteIfNeeded().isIdentifier() }

fun PsiNamedElement.isInterfaceClass(): Boolean = when (this) {
    is KtClass -> isInterface()
    is PsiClass -> isInterface
    is KtPsiClassWrapper -> psiClass.isInterface
    else -> false
}

fun KtNamedDeclaration.isAbstract(): Boolean = when {
    hasModifier(KtTokens.ABSTRACT_KEYWORD) -> true
    containingClassOrObject?.isInterfaceClass() != true -> false
    this is KtProperty -> initializer == null && delegate == null && accessors.isEmpty()
    this is KtNamedFunction -> !hasBody()
    else -> false
}

fun KtNamedDeclaration.isConstructorDeclaredProperty() = this is KtParameter && ownerFunction is KtPrimaryConstructor && hasValOrVar()

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

    val commonCount = min(oldCount, newCount)
    for (i in 0 until commonCount) {
        oldParameters[i] = oldParameters[i].replace(newParameters[i]) as KtElement
    }

    if (commonCount == 0) return originalList.listReplacer(newList)

    val lastOriginalParameter = oldParameters.last()

    if (oldCount > commonCount) {
        originalList.deleteChildRange(oldParameters[commonCount - 1].nextSibling, lastOriginalParameter)
    } else if (newCount > commonCount) {
        val psiBeforeLastParameter = lastOriginalParameter.prevSibling
        val withMultiline =
            (psiBeforeLastParameter is PsiWhiteSpace || psiBeforeLastParameter is PsiComment) && psiBeforeLastParameter.textContains('\n')
        val extraSpace = if (withMultiline) KtPsiFactory(originalList).createNewLine() else null
        originalList.addRangeAfter(newParameters[commonCount - 1].nextSibling, newParameters.last(), lastOriginalParameter)
        if (extraSpace != null) {
            val addedItems = originalList.itemsFun().subList(commonCount, newCount)
            for (addedItem in addedItems) {
                val elementBefore = addedItem.prevSibling
                if ((elementBefore !is PsiWhiteSpace && elementBefore !is PsiComment) || !elementBefore.textContains('\n')) {
                    addedItem.parent.addBefore(extraSpace, addedItem)
                }
            }
        }
    }

    return originalList
}

fun <T> Pass(body: (T) -> Unit) = object : Pass<T>() {
    override fun pass(t: T) = body(t)
}

fun KtExpression.removeTemplateEntryBracesIfPossible(): KtExpression {
    val parent = parent as? KtBlockStringTemplateEntry ?: return this
    val newEntry = if (parent.canDropBraces()) parent.dropBraces() else parent
    return newEntry.expression!!
}

fun dropOverrideKeywordIfNecessary(element: KtNamedDeclaration) {
    val callableDescriptor = element.resolveToDescriptorIfAny() as? CallableDescriptor ?: return
    if (callableDescriptor.overriddenDescriptors.isEmpty()) {
        element.removeModifier(KtTokens.OVERRIDE_KEYWORD)
    }
}

fun dropOperatorKeywordIfNecessary(element: KtNamedDeclaration) {
    val callableDescriptor = element.resolveToDescriptorIfAny() as? CallableDescriptor ?: return
    val diagnosticHolder = BindingTraceContext()
    OperatorModifierChecker.check(element, callableDescriptor, diagnosticHolder, element.languageVersionSettings)
    if (diagnosticHolder.bindingContext.diagnostics.any { it.factory == Errors.INAPPLICABLE_OPERATOR_MODIFIER }) {
        element.removeModifier(KtTokens.OPERATOR_KEYWORD)
    }
}

fun getQualifiedTypeArgumentList(initializer: KtExpression): KtTypeArgumentList? {
    val call = initializer.resolveToCall() ?: return null
    val typeArgumentMap = call.typeArguments
    val typeArguments = call.candidateDescriptor.typeParameters.mapNotNull { typeArgumentMap[it] }
    val renderedList = typeArguments.joinToString(prefix = "<", postfix = ">") {
        IdeDescriptorRenderers.SOURCE_CODE_NOT_NULL_TYPE_APPROXIMATION.renderType(it)
    }
    return KtPsiFactory(initializer).createTypeArguments(renderedList)
}

fun addTypeArgumentsIfNeeded(expression: KtExpression, typeArgumentList: KtTypeArgumentList) {
    val context = expression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
    val call = expression.getCallWithAssert(context)
    val callElement = call.callElement as? KtCallExpression ?: return
    if (call.typeArgumentList != null) return
    val callee = call.calleeExpression ?: return
    if (context.diagnostics.forElement(callee).all {
            it.factory != Errors.TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER &&
                    it.factory != Errors.NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
        }
    ) {
        return
    }

    callElement.addAfter(typeArgumentList, callElement.calleeExpression)
    ShortenReferences.DEFAULT.process(callElement.typeArgumentList!!)
}

internal fun DeclarationDescriptor.getThisLabelName(): String {
    if (!name.isSpecial) return name.asString()
    if (this is AnonymousFunctionDescriptor) {
        val function = source.getPsi() as? KtFunction
        val argument = function?.parent as? KtValueArgument
            ?: (function?.parent as? KtLambdaExpression)?.parent as? KtValueArgument
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

fun checkSuperMethods(
    declaration: KtDeclaration,
    ignore: Collection<PsiElement>?,
    actionString: String
): List<PsiElement> {
    fun getClassDescriptions(overriddenElementsToDescriptor: Map<PsiElement, CallableDescriptor>): List<String> {
        return overriddenElementsToDescriptor.entries.map { entry ->
            val (element, descriptor) = entry
            val description = when (element) {
                is KtNamedFunction, is KtProperty, is KtParameter -> formatClassDescriptor(descriptor.containingDeclaration)
                is PsiMethod -> {
                    val psiClass = element.containingClass ?: error("Invalid element: ${element.getText()}")
                    formatPsiClass(psiClass, markAsJava = true, inCode = false)
                }
                else -> error("Unexpected element: ${element.getElementTextWithContext()}")
            }
            "    $description\n"
        }
    }

    fun askUserForMethodsToSearch(
        declarationDescriptor: CallableDescriptor,
        overriddenElementsToDescriptor: Map<PsiElement, CallableDescriptor>
    ): List<PsiElement> {
        val superClassDescriptions = getClassDescriptions(overriddenElementsToDescriptor)

        val message = KotlinBundle.message(
            "x.overrides.y.in.class.list",
            DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.render(declarationDescriptor),
            "\n${superClassDescriptions.joinToString(separator = "")}",
            actionString
        )

        val exitCode = showYesNoCancelDialog(
            CHECK_SUPER_METHODS_YES_NO_DIALOG,
            declaration.project, message, IdeBundle.message("title.warning"), Messages.getQuestionIcon(), Messages.YES
        )
        return when (exitCode) {
            Messages.YES -> overriddenElementsToDescriptor.keys.toList()
            Messages.NO -> listOf(declaration)
            else -> emptyList()
        }
    }


    val declarationDescriptor = declaration.unsafeResolveToDescriptor() as CallableDescriptor

    if (declarationDescriptor is LocalVariableDescriptor) return listOf(declaration)

    val project = declaration.project
    val overriddenElementsToDescriptor = HashMap<PsiElement, CallableDescriptor>()
    for (overriddenDescriptor in DescriptorUtils.getAllOverriddenDescriptors(declarationDescriptor)) {
        val overriddenDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, overriddenDescriptor) ?: continue
        if (overriddenDeclaration is KtNamedFunction || overriddenDeclaration is KtProperty || overriddenDeclaration is PsiMethod || overriddenDeclaration is KtParameter) {
            overriddenElementsToDescriptor[overriddenDeclaration] = overriddenDescriptor
        }
    }
    if (ignore != null) {
        overriddenElementsToDescriptor.keys.removeAll(ignore)
    }

    if (overriddenElementsToDescriptor.isEmpty()) return listOf(declaration)

    return askUserForMethodsToSearch(declarationDescriptor, overriddenElementsToDescriptor)
}

fun checkSuperMethodsWithPopup(
    declaration: KtNamedDeclaration,
    deepestSuperMethods: List<PsiElement>,
    actionString: String,
    editor: Editor,
    action: (List<PsiElement>) -> Unit
) {
    if (deepestSuperMethods.isEmpty()) return action(listOf(declaration))

    val superMethod = deepestSuperMethods.first()

    val (superClass, isAbstract) = when (superMethod) {
        is PsiMember -> superMethod.containingClass to superMethod.hasModifierProperty(PsiModifier.ABSTRACT)
        is KtNamedDeclaration -> superMethod.containingClassOrObject to superMethod.isAbstract()
        else -> null
    } ?: return action(listOf(declaration))
    if (superClass == null) return action(listOf(declaration))

    if (ApplicationManager.getApplication().isUnitTestMode) return action(deepestSuperMethods)

    val kind = when (declaration) {
        is KtNamedFunction -> "function"
        is KtProperty, is KtParameter -> "property"
        else -> return
    }

    val unwrappedSupers = deepestSuperMethods.mapNotNull { it.namedUnwrappedElement }
    val hasJavaMethods = unwrappedSupers.any { it is PsiMethod }
    val hasKtMembers = unwrappedSupers.any { it is KtNamedDeclaration }
    val superKind = when {
        hasJavaMethods && hasKtMembers -> "member"
        hasJavaMethods -> "method"
        else -> kind
    }

    val renameBase = actionString + " base $superKind" + (if (deepestSuperMethods.size > 1) "s" else "")
    val renameCurrent = "$actionString only current $kind"
    val title = buildString {
        append(declaration.name)
        append(if (isAbstract) " implements " else " overrides ")
        append(ElementDescriptionUtil.getElementDescription(superMethod, UsageViewTypeLocation.INSTANCE))
        append(" of ")
        append(SymbolPresentationUtil.getSymbolPresentableText(superClass))
    }

    JBPopupFactory.getInstance()
        .createPopupChooserBuilder(listOf(renameBase, renameCurrent))
        .setTitle(title)
        .setMovable(false)
        .setResizable(false)
        .setRequestFocus(true)
        .setItemChosenCallback { selected ->
            if (selected == renameBase) {
                val ableToRename = declaration.project.let { project ->
                    deepestSuperMethods.all { PsiElementRenameHandler.canRename(project, editor, it) }
                }
                if (ableToRename) action(deepestSuperMethods + declaration)
            } else action(listOf(declaration))
        }
        .createPopup()
        .showInBestPositionFor(editor)
}

fun KtNamedDeclaration.isCompanionMemberOf(klass: KtClassOrObject): Boolean {
    val containingObject = containingClassOrObject as? KtObjectDeclaration ?: return false
    return containingObject.isCompanion() && containingObject.containingClassOrObject == klass
}

internal fun KtDeclaration.withExpectedActuals(): List<KtDeclaration> {
    val expect = liftToExpected() ?: return listOf(this)
    val actuals = expect.actualsForExpected()
    return listOf(expect) + actuals
}

internal fun KtDeclaration.resolveToExpectedDescriptorIfPossible(): DeclarationDescriptor {
    val descriptor = unsafeResolveToDescriptor()
    return descriptor.liftToExpected() ?: descriptor
}

fun DialogWrapper.showWithTransaction() {
    TransactionGuard.submitTransaction(disposable, Runnable { show() })
}

fun PsiMethod.checkDeclarationConflict(name: String, conflicts: MultiMap<PsiElement, String>, callables: Collection<PsiElement>) {
    containingClass
        ?.findMethodsByName(name, true)
        // as is necessary here: see KT-10386
        ?.firstOrNull { it.parameterList.parametersCount == 0 && !callables.contains(it.namedUnwrappedElement as PsiElement?) }
        ?.let { reportDeclarationConflict(conflicts, it) { s -> "$s already exists" } }
}

fun <T : KtExpression> T.replaceWithCopyWithResolveCheck(
    resolveStrategy: (T, BindingContext) -> DeclarationDescriptor?,
    context: BindingContext = analyze(),
    preHook: T.() -> Unit = {},
    postHook: T.() -> T? = { this }
): T? {
    val originDescriptor = resolveStrategy(this, context) ?: return null
    @Suppress("UNCHECKED_CAST") val elementCopy = copy() as T
    elementCopy.preHook()
    val newContext = elementCopy.analyzeAsReplacement(this, context)
    val newDescriptor = resolveStrategy(elementCopy, newContext) ?: return null

    return if (originDescriptor.canonicalRender() == newDescriptor.canonicalRender()) elementCopy.postHook() else null
}

@Deprecated(
    "Use org.jetbrains.kotlin.idea.core.util.getLineCount() instead",
    ReplaceWith("this.getLineCount()", "org.jetbrains.kotlin.idea.core.util.getLineCount"),
    DeprecationLevel.ERROR
)
fun PsiElement.getLineCount(): Int {
    return newGetLineCount()
}

@Deprecated(
    "Use org.jetbrains.kotlin.idea.core.util.toPsiDirectory() instead",
    ReplaceWith("this.toPsiDirectory()", "org.jetbrains.kotlin.idea.core.util.toPsiDirectory"),
    DeprecationLevel.ERROR
)
fun VirtualFile.toPsiDirectory(project: Project): PsiDirectory? {
    return newToPsiDirectory(project)
}

@Deprecated(
    "Use org.jetbrains.kotlin.idea.core.util.toPsiFile() instead",
    ReplaceWith("this.toPsiFile()", "org.jetbrains.kotlin.idea.core.util.toPsiFile"),
    DeprecationLevel.ERROR
)
fun VirtualFile.toPsiFile(project: Project): PsiFile? {
    return newToPsiFile(project)
}