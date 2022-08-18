/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmNames.JVM_NAME_SHORT
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.siblings
import javax.swing.Icon

abstract class KtLightClassForFacadeBase constructor(
    manager: PsiManager,
    override val facadeClassFqName: FqName,
    override val files: Collection<KtFile>
) : KtLightClassBase(manager), KtLightClassForFacade {
    private val firstFileInFacade by lazyPub { files.iterator().next() }

    private val modifierList: PsiModifierList =
        LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)

    private val implementsList: LightEmptyImplementsList =
        LightEmptyImplementsList(manager)

    private val packageClsFile by lazyPub {
        FakeFileForLightClass(
            firstFileInFacade,
            lightClass = { this },
            packageFqName = facadeClassFqName.parent()
        )
    }

    override fun getParent(): PsiElement = containingFile

    override val kotlinOrigin: KtClassOrObject? get() = null

    val fqName: FqName
        get() = facadeClassFqName

    override fun getModifierList() = modifierList

    override fun hasModifierProperty(@NonNls name: String) = modifierList.hasModifierProperty(name)

    override fun getExtendsList(): PsiReferenceList? = null

    override fun isDeprecated() = false

    override fun isInterface() = false

    override fun isAnnotationType() = false

    override fun isEnum() = false

    override fun getContainingClass(): PsiClass? = null

    override fun getContainingFile() = packageClsFile

    override fun hasTypeParameters() = false

    override fun getTypeParameters(): Array<out PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun getTypeParameterList(): PsiTypeParameterList? = null

    override fun getDocComment(): Nothing? = null

    override fun getImplementsList() = implementsList

    override fun getImplementsListTypes(): Array<out PsiClassType> = PsiClassType.EMPTY_ARRAY

    override fun getInterfaces(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getInnerClasses(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getOwnInnerClasses(): List<PsiClass> = listOf()

    override fun getAllInnerClasses(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getInitializers(): Array<out PsiClassInitializer> = PsiClassInitializer.EMPTY_ARRAY

    override fun findInnerClassByName(@NonNls name: String, checkBases: Boolean): PsiClass? = null

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false

    override fun getLBrace(): PsiElement? = null

    override fun getRBrace(): PsiElement? = null

    override fun getName() = super<KtLightClassForFacade>.getName()

    override fun setName(name: String): PsiElement? {
        for (file in files) {
            val jvmNameEntry = JvmFileClassUtil.findAnnotationEntryOnFileNoResolve(file, JVM_NAME_SHORT)

            if (PackagePartClassUtils.getFilePartShortName(file.name) == name) {
                jvmNameEntry?.delete()
                continue
            }

            if (jvmNameEntry == null) {
                val newFileName = PackagePartClassUtils.getFileNameByFacadeName(name)
                val facadeDir = file.parent
                if (newFileName != null && facadeDir != null && facadeDir.findFile(newFileName) == null) {
                    file.name = newFileName
                    continue
                }

                val psiFactory = KtPsiFactory(this)
                val annotationText = "${JVM_NAME_SHORT}(\"$name\")"
                val newFileAnnotationList = psiFactory.createFileAnnotationListWithAnnotation(annotationText)
                val annotationList = file.fileAnnotationList
                if (annotationList != null) {
                    annotationList.add(newFileAnnotationList.annotationEntries.first())
                } else {
                    val anchor = file.firstChild.siblings().firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
                    file.addBefore(newFileAnnotationList, anchor)
                }
                continue
            }

            val jvmNameExpression = jvmNameEntry.valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression
                ?: continue
            ElementManipulators.handleContentChange(jvmNameExpression, name)
        }

        return this
    }

    override fun getQualifiedName() = facadeClassFqName.asString()

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun isValid() = files.all { it.isValid && it.hasTopLevelCallables() && facadeClassFqName == it.javaFileFacadeFqName }

    abstract override fun copy(): KtLightClassForFacade

    override fun getNavigationElement() = firstFileInFacade

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        equals(another) ||
                (another is KtLightClassForFacade && another.facadeClassFqName == facadeClassFqName)

    override fun getElementIcon(flags: Int): Icon? = throw UnsupportedOperationException("This should be done by JetIconProvider")

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        return baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT
    }

    override fun getSuperClass(): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_LANG_OBJECT, resolveScope)
    }

    override fun getSupers(): Array<PsiClass> {
        return superClass?.let { arrayOf(it) } ?: arrayOf()
    }

    override fun getSuperTypes(): Array<PsiClassType> {
        return arrayOf(PsiType.getJavaLangObject(manager, resolveScope))
    }

    override fun hashCode() = facadeClassFqName.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other == null || this::class.java != other::class.java) {
            return false
        }

        val lightClass = other as KtLightClassForFacadeBase
        if (this === other) return true

        if (facadeClassFqName != lightClass.facadeClassFqName) return false
        if (files != lightClass.files) return false

        return true
    }

    override fun toString() = "${KtLightClassForFacadeBase::class.java.simpleName}:$facadeClassFqName"

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    override fun getText() = firstFileInFacade.text ?: ""

    override fun getTextRange(): TextRange = firstFileInFacade.textRange ?: TextRange.EMPTY_RANGE

    override fun getTextOffset() = firstFileInFacade.textOffset

    override fun getStartOffsetInParent() = firstFileInFacade.startOffsetInParent

    override fun isWritable() = files.all { it.isWritable }

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)
}
