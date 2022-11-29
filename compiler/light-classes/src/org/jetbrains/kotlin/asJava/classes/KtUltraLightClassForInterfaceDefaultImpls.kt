/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KtUltraLightClassForInterfaceDefaultImpls(classOrObject: KtClassOrObject, support: KtUltraLightSupport) :
    KtUltraLightClass(classOrObject, support) {
    override fun getQualifiedName(): String? = containingClass?.qualifiedName?.let { "$it.${JvmAbi.DEFAULT_IMPLS_CLASS_NAME}" }
    override fun getName() = JvmAbi.DEFAULT_IMPLS_CLASS_NAME
    override fun getParent() = containingClass
    override fun copy(): KtUltraLightClassForInterfaceDefaultImpls = KtUltraLightClassForInterfaceDefaultImpls(
        classOrObject.copy() as KtClassOrObject,
        support,
    )

    override fun getInterfaces(): Array<PsiClass> = emptyArray()

    override fun createExtendsList(): PsiReferenceList? = null
    override fun createImplementsList(): PsiReferenceList? = null
    override fun getSuperClass(): PsiClass? = PsiClassImplUtil.getSuperClass(this)
    override fun getSupers(): Array<PsiClass> = PsiClassImplUtil.getSuperClass(this)?.let { arrayOf(it) } ?: emptyArray()
    override fun getSuperTypes(): Array<PsiClassType> = arrayOf(PsiType.getJavaLangObject(manager, resolveScope))

    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = emptyArray()
    override fun computeModifiers(): Set<String> = publicStaticFinal

    override fun getOwnFields(): List<KtLightField> = emptyList()
    override fun isInterface(): Boolean = false

    override fun isDeprecated(): Boolean = false
    override fun isAnnotationType(): Boolean = false
    override fun isEnum(): Boolean = false
    override fun isFinal(isFinalByPsi: Boolean): Boolean = true
    override fun hasTypeParameters(): Boolean = false
    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean =
        baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT

    override fun setName(name: String): PsiElement =
        throw IncorrectOperationException("Impossible to rename ${JvmAbi.DEFAULT_IMPLS_CLASS_NAME}")

    override fun getContainingClass(): KtLightClass? = classOrObject.toLightClass()

    override fun getOwnInnerClasses() = emptyList<PsiClass>()
    override fun getOwnMethods(): List<KtLightMethod> = _ownMethods.value

    private val membersBuilder by lazyPub {
        UltraLightMembersCreator(
            this,
            false,
            classOrObject.hasModifier(KtTokens.SEALED_KEYWORD),
            mangleInternalFunctions = true,
            support = support,
        )
    }


    private fun ownMethods(): List<KtLightMethod> {
        val interfaceType by lazy {
            JavaPsiFacade.getElementFactory(project).createType(containingClass!!)
        }

        val result = mutableListOf<KtLightMethod>()
        fun processClass(psiClass: PsiClass) {
            val declarations = psiClass.safeAs<KtUltraLightClass>()
                ?.kotlinOrigin
                ?.declarations
                ?.filterNot { it.isHiddenByDeprecation(support) }
                .orEmpty()

            for (declaration in declarations) {
                when (declaration) {
                    is KtNamedFunction -> {
                        if (declaration.hasBody()) {
                            result.addAll(
                                membersBuilder.createMethods(declaration, forceStatic = true, forceNonFinal = true) {
                                    KtUltraLightReceiverParameterForDefaultImpls(support, it) { interfaceType }
                                }
                            )
                        }
                    }

                    is KtProperty -> {
                        if (declaration.accessors.any(KtPropertyAccessor::hasBody)) {
                            result.addAll(
                                membersBuilder.propertyAccessors(
                                    declaration,
                                    declaration.isVar,
                                    forceStatic = true,
                                    onlyJvmStatic = false,
                                    forceNonFinal = true,
                                ) {
                                    KtUltraLightReceiverParameterForDefaultImpls(support, it) { interfaceType }
                                }
                            )
                        }
                    }
                }
            }

            for (superClass in psiClass.interfaces) {
                processClass(superClass)
            }
        }

        containingClass?.let { processClass(it) }

        return result
    }

    private val _ownMethods: CachedValue<List<KtLightMethod>> = CachedValuesManager.getManager(project).createCachedValue(
        /* provider = */
        {
            CachedValueProvider.Result.create(
                ownMethods(),
                classOrObject.getExternalDependencies()
            )
        },
        /* trackValue = */ false,
    )
}

internal class KtUltraLightReceiverParameterForDefaultImpls(
    support: KtUltraLightSupport,
    method: KtUltraLightMethod,
    private val typeGetter: () -> PsiType,
) : KtUltraLightParameter(AsmUtil.THIS_IN_DEFAULT_IMPLS, null, support, method) {
    override fun getType(): PsiType = typeGetter()
    override fun isVarArgs(): Boolean = false
    override val qualifiedNameForNullabilityAnnotation: String = NotNull::class.java.name
}

private val publicStaticFinal = setOf(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL)