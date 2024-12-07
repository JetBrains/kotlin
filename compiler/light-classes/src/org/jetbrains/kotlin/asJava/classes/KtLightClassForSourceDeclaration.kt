/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiImmediateClassType
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import javax.swing.Icon

abstract class KtLightClassForSourceDeclaration(
    protected val classOrObject: KtClassOrObject,
    protected val jvmDefaultMode: JvmDefaultMode,
) : KtLightClassBase(classOrObject.manager),
    StubBasedPsiElement<KotlinClassOrObjectStub<out KtClassOrObject>> {
    override fun cacheDependencies(): List<Any> = classOrObject.getExternalDependencies()

    override fun getText() = kotlinOrigin.text ?: ""

    override fun getTextRange(): TextRange? = kotlinOrigin.textRange ?: TextRange.EMPTY_RANGE

    override fun getTextOffset() = kotlinOrigin.textOffset

    override fun getStartOffsetInParent() = kotlinOrigin.startOffsetInParent

    override fun isWritable() = kotlinOrigin.isWritable

    private val _extendsList by lazyPub { createExtendsList() }
    private val _implementsList by lazyPub { createImplementsList() }

    protected abstract fun createExtendsList(): PsiReferenceList?
    protected abstract fun createImplementsList(): PsiReferenceList?

    override val kotlinOrigin: KtClassOrObject = classOrObject

    abstract override fun copy(): PsiElement
    abstract override fun getParent(): PsiElement?
    abstract override fun getQualifiedName(): String?

    abstract override fun getContainingFile(): PsiFile?

    override fun getNavigationElement(): PsiElement = classOrObject

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        kotlinOrigin.isEquivalentTo(another) ||
                equals(another) ||
                (qualifiedName != null && another is KtLightClassForSourceDeclaration && qualifiedName == another.qualifiedName)

    override fun getElementIcon(flags: Int): Icon? =
        throw UnsupportedOperationException("This should be done by KotlinIconProvider")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.java != other::class.java) return false

        val aClass = other as KtLightClassForSourceDeclaration

        if (jvmDefaultMode != aClass.jvmDefaultMode) return false
        if (classOrObject != aClass.classOrObject) return false

        return true
    }

    override fun hashCode(): Int = classOrObject.hashCode() * 31 + jvmDefaultMode.hashCode()


    private val _typeParameterList: PsiTypeParameterList by lazyPub { buildTypeParameterList() }

    protected abstract fun buildTypeParameterList(): PsiTypeParameterList

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList

    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList.typeParameters

    override fun getName(): String? = classOrObject.nameAsName?.asString()

    abstract override fun getModifierList(): PsiModifierList?

    override fun hasModifierProperty(@NonNls name: String): Boolean = modifierList?.hasModifierProperty(name) ?: false
    abstract override fun isDeprecated(): Boolean

    override fun isInterface(): Boolean {
        if (classOrObject !is KtClass) return false
        return classOrObject.isInterface() || classOrObject.isAnnotation()
    }

    override fun isAnnotationType(): Boolean = classOrObject is KtClass && classOrObject.isAnnotation()

    override fun isEnum(): Boolean = classOrObject is KtClass && classOrObject.isEnum()

    override fun hasTypeParameters(): Boolean = classOrObject is KtClass && classOrObject.typeParameters.isNotEmpty()

    override fun isValid(): Boolean = classOrObject.isValid

    abstract override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean

    @Throws(IncorrectOperationException::class)
    override fun setName(@NonNls name: String): PsiElement {
        kotlinOrigin.setName(name)
        return this
    }

    override fun toString() = "${this::class.java.simpleName}:${classOrObject.getDebugText()}"

    abstract override fun getOwnInnerClasses(): List<PsiClass>

    override fun getUseScope(): SearchScope = kotlinOrigin.useScope

    override fun getElementType(): IStubElementType<out StubElement<*>, *>? = classOrObject.elementType
    override fun getStub(): KotlinClassOrObjectStub<out KtClassOrObject>? = classOrObject.stub

    override fun getNameIdentifier(): KtLightIdentifier? = KtLightIdentifier(this, classOrObject)

    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun getImplementsList(): PsiReferenceList? = _implementsList

    abstract override fun getSupers(): Array<PsiClass>

    abstract override fun getSuperTypes(): Array<PsiClassType>

    private fun getSupertypeByPsi(): PsiImmediateClassType? {
        return classOrObject.defaultJavaAncestorQualifiedName()?.let { ancestorFqName ->
            JavaPsiFacade.getInstance(project).findClass(ancestorFqName, resolveScope)?.let {
                PsiImmediateClassType(it, createSubstitutor(it))
            }
        }
    }

    private fun createSubstitutor(ancestor: PsiClass): PsiSubstitutor {
        if (ancestor.qualifiedName != CommonClassNames.JAVA_LANG_ENUM) {
            return PsiSubstitutor.EMPTY
        }
        val javaLangEnumsTypeParameter = ancestor.typeParameters.firstOrNull() ?: return PsiSubstitutor.EMPTY
        return PsiSubstitutor.createSubstitutor(
            mapOf(
                javaLangEnumsTypeParameter to PsiImmediateClassType(this, PsiSubstitutor.EMPTY)
            )
        )
    }

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    abstract fun isFinal(isFinalByPsi: Boolean): Boolean
}

fun KtLightClassForSourceDeclaration.isPossiblyAffectedByAllOpen() =
    !isAnnotationType && !isInterface && kotlinOrigin.annotationEntries.isNotEmpty()
