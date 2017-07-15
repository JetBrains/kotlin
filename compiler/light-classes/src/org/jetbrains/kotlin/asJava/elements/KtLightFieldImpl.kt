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

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import com.intellij.psi.impl.PsiVariableEx
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForEnumEntry
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.lang.UnsupportedOperationException

sealed class KtLightFieldImpl<D : PsiField>(
        override val lightMemberOrigin: LightMemberOrigin?,
        computeRealDelegate: () -> D,
        containingClass: KtLightClass,
        dummyDelegate: PsiField?
) : KtLightMemberImpl<PsiField>(computeRealDelegate, lightMemberOrigin, containingClass, dummyDelegate), KtLightField {

    override val clsDelegate: D
        @Suppress("UNCHECKED_CAST")
        get() = super.clsDelegate as D

    override fun setInitializer(initializer: PsiExpression?) = cannotModify()

    override fun getType() = clsDelegate.type

    override fun getTypeElement() = clsDelegate.typeElement

    override fun getInitializer() = clsDelegate.initializer

    override fun hasInitializer() = clsDelegate.hasInitializer()

    override fun normalizeDeclaration() = cannotModify()

    override fun computeConstantValue() = clsDelegate.computeConstantValue()

    override fun setName(@NonNls name: String): PsiElement {
        (kotlinOrigin as? KtNamedDeclaration)?.setName(name)
        return this
    }

    override fun equals(other: Any?): Boolean =
            this === other ||
            (other is KtLightFieldImpl<*> &&
             this.name == other.name &&
             this.containingClass == other.containingClass)

    override fun hashCode() = 31 * containingClass.hashCode() + name.hashCode()

    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?): Any? {
        return (clsDelegate as PsiVariableEx).computeConstantValue(visitedVars)
    }

    override fun copy() = Factory.create(lightMemberOrigin?.copy(), clsDelegate, containingClass)


    class KtLightEnumConstant(
            origin: LightMemberOrigin?,
            computeDelegate: () -> PsiEnumConstant,
            containingClass: KtLightClass,
            dummyDelegate: PsiField?
    ) : KtLightFieldImpl<PsiEnumConstant>(origin, computeDelegate, containingClass, dummyDelegate), PsiEnumConstant {
        private val initializingClass by lazyPub {
            val kotlinEnumEntry = (lightMemberOrigin as? LightMemberOriginForDeclaration)?.originalElement as? KtEnumEntry
            if (kotlinEnumEntry != null && kotlinEnumEntry.declarations.isNotEmpty()) {
                KtLightClassForEnumEntry(kotlinEnumEntry, clsDelegate)
            }
            else null
        }

        // NOTE: we don't use "delegation by" because the compiler would generate method calls to ALL of PsiEnumConstant members,
        // but we need only members whose implementations are not present in KotlinLightField
        override fun getArgumentList() = clsDelegate.argumentList

        override fun getInitializingClass(): PsiEnumConstantInitializer? = initializingClass
        override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer {
            return initializingClass ?: throw UnsupportedOperationException("Can't create enum constant body: ${clsDelegate.name}")
        }

        override fun resolveConstructor() = clsDelegate.resolveConstructor()
        override fun resolveMethod() = clsDelegate.resolveMethod()
        override fun resolveMethodGenerics() = clsDelegate.resolveMethodGenerics()
    }

    class KtLightFieldForDeclaration(origin: LightMemberOrigin?, computeDelegate: () -> PsiField, containingClass: KtLightClass, dummyDelegate: PsiField?) :
            KtLightFieldImpl<PsiField>(origin, computeDelegate, containingClass, dummyDelegate)

    companion object Factory {
        fun create(origin: LightMemberOrigin?, delegate: PsiField, containingClass: KtLightClass): KtLightField = when (delegate) {
            is PsiEnumConstant -> KtLightEnumConstant(origin, { delegate }, containingClass, null)
            else -> KtLightFieldForDeclaration(origin, { delegate }, containingClass, null)
        }

        fun lazy(
                dummyDelegate: PsiField,
                origin: LightMemberOriginForDeclaration,
                containingClass: KtLightClass,
                computeRealDelegate: () -> PsiField
        ): KtLightField {
            if (dummyDelegate is PsiEnumConstant) {
                @Suppress("UNCHECKED_CAST")
                return KtLightEnumConstant(origin, computeRealDelegate as () -> PsiEnumConstant, containingClass, dummyDelegate)
            }
            return KtLightFieldForDeclaration(origin, computeRealDelegate, containingClass, dummyDelegate)
        }

        fun fromClsFields(delegateClass: PsiClass, containingClass: KtLightClass) = delegateClass.fields.map {
            KtLightFieldImpl.create(getOrigin(it), it, containingClass)
        }

        fun getOrigin(field: PsiField) = getMemberOrigin(field)
    }
}
