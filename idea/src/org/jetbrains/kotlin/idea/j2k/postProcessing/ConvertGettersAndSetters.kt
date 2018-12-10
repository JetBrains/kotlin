/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.j2k.postProcessing

import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessing
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.setModifierList
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

class ConvertGettersAndSetters : J2kPostProcessing {
    override val writeActionNeeded: Boolean = true

    data class PropertyData(
        var name: String,
        var getter: Accessor? = null,
        var setter: Accessor? = null
    )

    data class Accessor(
        val name: String,
        val target: KtProperty?,
        val function: KtFunction
    )

    private fun Accessor.isTrivial() = target != null

    private fun KtExpression.statements() =
        if (this is KtBlockExpression) statements
        else listOf(this)

    private fun KtFunction.asGetter(): Accessor? {
        if (valueParameters.isNotEmpty()) return null
        if (typeParameters.isNotEmpty()) return null
        val name = getterName() ?: return null
        val target = bodyExpression
            ?.statements()
            ?.singleOrNull()
            ?.let {
                if (it is KtReturnExpression) it.returnedExpression
                else it
            }?.let {
                it.unpackedReferenceToProperty()
            }?.takeIf {
                it.type() == this.type()
            }
        return Accessor(name, target, this)
    }

    private fun KtFunction.isProcedure() =
        bodyExpression is KtBlockExpression? && !hasDeclaredReturnType()
                || getReturnTypeReference()?.typeElement!!.text == "Unit"

    private fun KtFunction.asSetter(): Accessor? {
        if (typeParameters.isNotEmpty()) return null
        if (valueParameters.size != 1) return null
        if (!isProcedure()) return null
        val name = setterName() ?: return null

        val target = bodyExpression
            ?.statements()
            ?.singleOrNull()
            ?.let {
                if (it is KtBinaryExpression) {
                    if (it.operationToken != KtTokens.EQEQ) return@let null
                    val right = it.right as? KtNameReferenceExpression ?: return@let null
                    if (right.reference?.resolve() != valueParameters.single()) return@let null
                    it.left?.unpackedReferenceToProperty()
                } else null
            }?.takeIf {
                it.type() == valueParameters.single().type()
            }
        return Accessor(name, target, this)
    }

    private val keywords = KtTokens.KEYWORDS.types.map { (it as KtKeywordToken).value }.toSet()

    private fun String.escaped() =
        if (this in keywords) "`$this`"
        else this

    private fun KtFunction.getterName() =
        name?.takeIf { JvmAbi.isGetterName(it) }
            ?.removePrefix("get")
            ?.takeIf {
                it.isNotEmpty() && it.first().isUpperCase()
                        || it.startsWith("is") && it.length > 2 && it[2].isUpperCase()
            }?.decapitalize()
            ?.escaped()

    private fun KtFunction.setterName() =
        name?.takeIf { JvmAbi.isSetterName(it) }
            ?.removePrefix("set")
            ?.takeIf { it.first().isUpperCase() }
            ?.decapitalize()
            ?.escaped()

    private fun generatePropertiesData(element: KtClassOrObject): List<PropertyData> {
        val properties = mutableMapOf<String, PropertyData>()

        for (declaration in element.declarations) {
            when (declaration) {
                is KtFunction -> {
                    declaration.asGetter()
                        ?.also { getter ->
                            properties.getOrPut(getter.name.removePrefix("is").decapitalize()) {
                                PropertyData(getter.name)
                            }.also { it.getter = getter }
                        }

                    declaration.asSetter()
                        ?.also { setter ->
                            properties.getOrPut(setter.name) {
                                PropertyData(setter.name)
                            }.also { it.setter = setter }
                        }
                }
            }
        }
        return properties.values.toList()
    }

    private fun <T : KtExpression> T.replaceBodyText(
        from: KtElement,
        to: KtExpression,
        replaceOnlyWriteUsages: Boolean
    ): T =
        also {
            from.usages()
                .map { it.element }
                .filter { it.isInsideOf(listOf(this)) }
                .forEach { reference ->
                    val parent = reference.parent
                    val referenceExpression = when {
                        parent is KtQualifiedExpression && parent.receiverExpression is KtThisExpression ->
                            parent
                        else -> reference
                    }
                    if (!replaceOnlyWriteUsages
                        || (referenceExpression.parent as? KtExpression)?.asAssignment()?.left == referenceExpression
                    ) {
                        referenceExpression.replace(to)
                    }
                }
        }


    private fun KtPsiFactory.createGetter(expression: KtExpression?): KtPropertyAccessor {
        val property =
            createProperty("val x get" + if (expression == null) "" else if (expression is KtBlockExpression) "() { return 1 }" else "() = 1")
        val getter = property.getter!!
        val bodyExpression = getter.bodyExpression

        bodyExpression?.replace(expression!!)
        return getter
    }


    private fun KtProperty.addGetter(getterFunction: Accessor, target: KtProperty): KtPropertyAccessor {
        val factory = KtPsiFactory(this)
        val newBody =
            if (!getterFunction.isTrivial()) {
                getterFunction.function.bodyExpression
                    ?.replaceBodyText(target, factory.createExpression("field"), replaceOnlyWriteUsages = false)
            } else null
        add(factory.createGetter(newBody))
        getterFunction.function.modifierList?.also {
            getter!!.addModifiers(it)
        }
        return getter!!
    }

    private fun KtPropertyAccessor.addModifiers(newModifiers: KtModifierList) {
        setModifierList(newModifiers)
        removeModifier(KtTokens.OVERRIDE_KEYWORD)
        removeModifier(KtTokens.FINAL_KEYWORD)
    }

    private fun KtPsiFactory.createSetter(body: KtExpression?, fieldName: String): KtPropertyAccessor {
        val property = when (body) {
            null -> createProperty("var x = 1\n  get() = 1\n set")
            is KtBlockExpression -> createProperty("var x get() = 1\nset($fieldName) {\n field = $fieldName\n }")
            else -> createProperty("var x get() = 1\nset($fieldName) = TODO()")
        }
        val setter = property.setter!!
        if (body != null) {
            setter.bodyExpression?.replace(body)
        }
        return setter
    }

    private fun KtProperty.addSetter(setterInfo: Accessor): KtPropertyAccessor {
        val factory = KtPsiFactory(this)
        val newBody =
            if (!setterInfo.isTrivial()) {
                setterInfo.function.bodyExpression?.replaceBodyText(this, factory.createExpression("field"), true)
            } else null
        add(factory.createSetter(newBody, setterInfo.function.valueParameters.single().name!!))
        setterInfo.function.modifierList?.also {
            setter!!.addModifiers(it)
        }
        return setter!!
    }

    private fun KtElement.forAllUsages(action: (KtElement) -> Unit) {
        ReferencesSearch.search(this, LocalSearchScope(containingKtFile))
            .forEach { action(it.element as KtElement) }
    }

    private fun KtElement.usages() =
        ReferencesSearch.search(this, LocalSearchScope(containingKtFile))
            .findAll()


    override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
        if (element !is KtClassOrObject) return null

        return {
            val factory = KtPsiFactory(element)
            for ((name, getterFunction, setterFunction) in generatePropertiesData(element)) {
                if (getterFunction == null) continue

                if (!getterFunction.isTrivial() &&
                    element.declarations.any { (it as? KtProperty)?.name == name }
                ) {//TODO improve??
                    continue
                }

                val setterAndGetterHaveTheSameTypes =
                    setterFunction != null &&
                            setterFunction.function.valueParameters.single().type() == getterFunction.function.type()

                if (element.isInterfaceClass() && setterFunction != null && !setterAndGetterHaveTheSameTypes) {
                    continue
                }

                val hasOverrideModifier =
                    getterFunction.target?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false
                            || getterFunction.function.hasModifier(KtTokens.OVERRIDE_KEYWORD)
                            || setterFunction?.function?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false

                val target = getterFunction.target
                    ?: factory.createProperty(
                        name,
                        getterFunction.function.getReturnTypeReference()?.text,
                        true
                    )

                if (target.name != name) {//TODO use refactoring
                    target.forAllUsages { usage ->
                        val parent = (usage.parent as? KtExpression)
                            ?.asAssignment()
                            ?.takeIf { it.left == usage }

                        val expression = if (parent != null) factory.createExpression("this.$name")
                        else factory.createExpression(name)

                        usage.replace(expression)
                    }
                    target.setName(name)
                }

                val propertyGetter = getterFunction.let {
                    val getter = target.addGetter(it, target)
                    it.function.forAllUsages { usage ->
                        usage.parentOfType<KtCallExpression>()!!.replace(factory.createExpression(name))
                    }
                    getter
                }

                val propertySetter = setterFunction
                    ?.takeIf { setterAndGetterHaveTheSameTypes }
                    ?.let {
                        val setter = target.addSetter(it)
                        it.function.forAllUsages { usage ->
                            val callExpression = usage.parentOfType<KtCallExpression>()!!
                            val qualifier = callExpression.getQualifiedExpressionForSelector()
                            val newValue = callExpression.valueArguments.single()
                            if (qualifier != null) {
                                qualifier
                                    .replace(factory.createExpression("${qualifier.receiverExpression.text}.${target.name} = ${newValue.text}"))
                            } else {
                                callExpression.replace(factory.createExpression("${target.name} = ${newValue.text}"))
                            }
                        }
                        it.function.delete()
                        setter
                    }
                    ?: if (target.isVar
                        && target.visibilityModifierTypeOrDefault() in listOf(KtTokens.PRIVATE_KEYWORD, KtTokens.PROTECTED_KEYWORD)
                    ) {
                        factory.createSetter(null, "value").apply {
                            setVisibility(target.visibilityModifierTypeOrDefault())
                            target.add(this)
                        }
                        target.setter!!
                    } else null


                val isVar = propertySetter != null

                if (target.isVar != isVar) {
                    target.valOrVarKeyword
                        .replace(if (isVar) factory.createVarKeyword() else factory.createValKeyword())
                }


                val visibility = listOfNotNull(
                    propertySetter?.visibilityModifierTypeOrDefault(),
                    propertyGetter.visibilityModifierTypeOrDefault(),
                    target.visibilityModifierTypeOrDefault()
                ).maxBy {
                    when (it) {
                        KtTokens.PUBLIC_KEYWORD -> 4
                        KtTokens.INTERNAL_KEYWORD -> 3
                        KtTokens.PROTECTED_KEYWORD -> 2
                        KtTokens.PRIVATE_KEYWORD -> 1
                        else -> 0
                    }
                }!!
                target.setVisibility(visibility)

                if (getterFunction.target == null) {
                    element.addDeclarationAfter(target, getterFunction.function)
                    getterFunction.function.delete()
                } else {
                    getterFunction.function.delete()
                }
                if (!target.hasModifier(KtTokens.OVERRIDE_KEYWORD) && hasOverrideModifier) {
                    target.addModifier(KtTokens.OVERRIDE_KEYWORD)
                }
            }
        }
    }
}