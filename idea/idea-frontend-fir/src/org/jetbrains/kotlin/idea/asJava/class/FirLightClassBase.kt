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

package org.jetbrains.kotlin.idea.asJava

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.util.Pair
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.classes.KotlinClassInnerStuffCache
import org.jetbrains.kotlin.asJava.classes.KotlinClassInnerStuffCache.Companion.processDeclarationsInEnum
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.KotlinLanguage

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtAnnotatedSymbol
import java.util.*

abstract class FirLightClassBase protected constructor(manager: PsiManager) : LightElement(manager, KotlinLanguage.INSTANCE), PsiClass,
    KtLightClass, PsiExtensibleClass {

    override val clsDelegate: PsiClass
        get() = invalidAccess()

    protected open val myInnersCache = KotlinClassInnerStuffCache(
        myClass = this,
        externalDependencies = listOf(KotlinModificationTrackerService.getInstance(manager.project).outOfBlockModificationTracker)
    )

    override fun getFields(): Array<PsiField> = myInnersCache.fields

    override fun getMethods(): Array<PsiMethod> = myInnersCache.methods

    override fun getConstructors(): Array<PsiMethod> = myInnersCache.constructors

    override fun getInnerClasses(): Array<out PsiClass> = myInnersCache.innerClasses

    override fun getAllFields(): Array<PsiField> = PsiClassImplUtil.getAllFields(this)

    override fun getAllMethods(): Array<PsiMethod> = PsiClassImplUtil.getAllMethods(this)

    override fun getAllInnerClasses(): Array<PsiClass> = PsiClassImplUtil.getAllInnerClasses(this)

    override fun findFieldByName(name: String, checkBases: Boolean) =
        myInnersCache.findFieldByName(name, checkBases)

    override fun findMethodsByName(name: String, checkBases: Boolean): Array<PsiMethod> =
        myInnersCache.findMethodsByName(name, checkBases)

    override fun findInnerClassByName(name: String, checkBases: Boolean): PsiClass? =
        myInnersCache.findInnerClassByName(name, checkBases)

    override fun processDeclarations(
        processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement
    ): Boolean {

        if (isEnum && !processDeclarationsInEnum(processor, state, myInnersCache)) return false

        return PsiClassImplUtil.processDeclarationsInClass(
            this,
            processor,
            state,
            null,
            lastParent,
            place,
            PsiUtil.getLanguageLevel(place),
            false
        )
    }

    override fun getText(): String = kotlinOrigin?.text ?: ""

    override fun getLanguage(): KotlinLanguage = KotlinLanguage.INSTANCE

    override fun getPresentation(): ItemPresentation? =
        ItemPresentationProviders.getItemPresentation(this)

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    override fun getContext(): PsiElement = parent

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        PsiClassImplUtil.isClassEquivalentTo(this, another)

    override fun getDocComment(): PsiDocComment? = null

    override fun hasTypeParameters(): Boolean = PsiImplUtil.hasTypeParameters(this)

    override fun getExtendsListTypes(): Array<PsiClassType?> =
        PsiClassImplUtil.getExtendsListTypes(this)

    override fun getImplementsListTypes(): Array<PsiClassType?> =
        PsiClassImplUtil.getImplementsListTypes(this)

    override fun findMethodBySignature(patternMethod: PsiMethod?, checkBases: Boolean): PsiMethod? =
        patternMethod?.let { PsiClassImplUtil.findMethodBySignature(this, it, checkBases) }

    override fun findMethodsBySignature(patternMethod: PsiMethod?, checkBases: Boolean): Array<PsiMethod?> =
        patternMethod?.let { PsiClassImplUtil.findMethodsBySignature(this, it, checkBases) } ?: emptyArray()

    override fun findMethodsAndTheirSubstitutorsByName(
        @NonNls name: String?,
        checkBases: Boolean
    ): List<Pair<PsiMethod?, PsiSubstitutor?>?> =
        PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases)

    override fun getAllMethodsAndTheirSubstitutors(): List<Pair<PsiMethod?, PsiSubstitutor?>?> {
        return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD)
    }

    abstract override fun copy(): PsiElement

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitClass(this)
        } else {
            visitor.visitElement(this)
        }
    }

    protected fun createMethods(declarations: Sequence<KtCallableSymbol>, isTopLevel: Boolean, result: MutableList<KtLightMethod>) {
        //TODO isHiddenByDeprecation
        var methodIndex = METHOD_INDEX_BASE
        for (declaration in declarations) {

            if (declaration is KtAnnotatedSymbol && declaration.hasAnnotation("kotlin.jvm.JvmSynthetic")) continue

            when (declaration) {
                is KtFunctionSymbol -> {
                    result.add(
                        FirLightSimpleMethodForSymbol(
                            functionSymbol = declaration,
                            lightMemberOrigin = null,
                            containingClass = this@FirLightClassBase,
                            isTopLevel = isTopLevel,
                            methodIndex = methodIndex++
                        )
                    )

                    if (declaration.hasAnnotation("kotlin.jvm.JvmOverloads")) {
                        val skipMask = BitSet(declaration.valueParameters.size)

                        for (i in declaration.valueParameters.size - 1 downTo 0) {

                            if (!declaration.valueParameters[i].hasDefaultValue) continue

                            skipMask.set(i)

                            result.add(
                                FirLightSimpleMethodForSymbol(
                                    functionSymbol = declaration,
                                    lightMemberOrigin = null,
                                    containingClass = this@FirLightClassBase,
                                    isTopLevel = isTopLevel,
                                    methodIndex = methodIndex++,
                                    argumentsSkipMask = skipMask
                                )
                            )
                        }
                    }
                }
                is KtConstructorSymbol -> {
                    result.add(
                        FirLightConstructorForSymbol(
                            constructorSymbol = declaration,
                            lightMemberOrigin = null,
                            containingClass = this@FirLightClassBase,
                            methodIndex++
                        )
                    )
                }
                is KtPropertySymbol -> {

                    if (declaration.hasAnnotation("kotlin.jvm.JvmField")) continue

                    val getter = declaration.getter?.takeIf {
                        !declaration.hasAnnotation("kotlin.jvm.JvmSynthetic", AnnotationUseSiteTarget.PROPERTY_GETTER)
                    }

                    if (getter != null) {
                        result.add(
                            FirLightAccessorMethodForSymbol(
                                propertyAccessorSymbol = getter,
                                firContainingProperty = declaration,
                                lightMemberOrigin = null,
                                containingClass = this@FirLightClassBase,
                                isTopLevel = isTopLevel
                            )
                        )
                    }

                    val setter = declaration.setter?.takeIf {
                        !declaration.hasAnnotation("kotlin.jvm.JvmSynthetic", AnnotationUseSiteTarget.PROPERTY_SETTER)
                    }

                    if (setter != null) {
                        result.add(
                            FirLightAccessorMethodForSymbol(
                                propertyAccessorSymbol = setter,
                                firContainingProperty = declaration,
                                lightMemberOrigin = null,
                                containingClass = this@FirLightClassBase,
                                isTopLevel = isTopLevel
                            )
                        )
                    }
                }
            }
        }
    }

    protected fun createFields(declarations: Sequence<KtCallableSymbol>, isTopLevel: Boolean, result: MutableList<KtLightField>) {
        //TODO isHiddenByDeprecation
        for (declaration in declarations) {
            if (declaration !is KtPropertySymbol) continue

            if (!declaration.hasBackingField) continue
            if (declaration.hasAnnotation("kotlin.jvm.JvmSynthetic")) continue
            result.add(
                FirLightFieldForPropertySymbol(
                    propertySymbol = declaration,
                    containingClass = this@FirLightClassBase,
                    lightMemberOrigin = null,
                    isTopLevel = isTopLevel
                )
            )
        }
    }
}