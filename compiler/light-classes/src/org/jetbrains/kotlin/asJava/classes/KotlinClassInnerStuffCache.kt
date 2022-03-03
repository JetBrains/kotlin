/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.*
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.impl.source.tree.java.PsiCompositeModifierList
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import com.intellij.psi.util.PsiUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException
import gnu.trove.THashMap
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class KotlinClassInnerStuffCache(
    private val myClass: PsiExtensibleClass,
    private val dependencies: List<Any>,
    private val lazyCreator: LazyCreator,
) {
    abstract class LazyCreator {
        abstract fun <T : Any> get(initializer: () -> T, dependencies: List<Any>): Lazy<T>
    }

    private fun <T : Any> cache(initializer: () -> T): Lazy<T> = lazyCreator.get(initializer, dependencies)

    private val constructorsCache = cache { PsiImplUtil.getConstructors(myClass) }

    val constructors: Array<PsiMethod>
        get() = copy(constructorsCache.value)

    private val fieldsCache = cache {
        val own = myClass.ownFields
        val ext = collectAugments(myClass, PsiField::class.java)
        ArrayUtil.mergeCollections(own, ext, PsiField.ARRAY_FACTORY)
    }

    val fields: Array<PsiField>
        get() = copy(fieldsCache.value)

    private val methodsCache = cache {
        val own = myClass.ownMethods
        var ext = collectAugments(myClass, PsiMethod::class.java)
        if (myClass.isEnum) {
            ext = ArrayList<PsiMethod>(ext.size + 2).also {
                it += ext
                it.addIfNotNull(getValuesMethod())
                it.addIfNotNull(getValueOfMethod())
            }
        }
        ArrayUtil.mergeCollections(own, ext, PsiMethod.ARRAY_FACTORY)
    }

    val methods: Array<PsiMethod>
        get() = copy(methodsCache.value)

    private val innerClassesCache = cache {
        val own = myClass.ownInnerClasses
        val ext = collectAugments(myClass, PsiClass::class.java)
        ArrayUtil.mergeCollections(own, ext, PsiClass.ARRAY_FACTORY)
    }

    val innerClasses: Array<out PsiClass>
        get() = copy(innerClassesCache.value)

    private val recordComponentsCache = cache {
        val header = myClass.recordHeader
        if (header == null) PsiRecordComponent.EMPTY_ARRAY else header.recordComponents
    }

    val recordComponents: Array<PsiRecordComponent>
        get() = copy(recordComponentsCache.value)

    private val fieldByNameCache = cache {
        val fields = this.fields.takeIf { it.isNotEmpty() } ?: return@cache emptyMap()
        Collections.unmodifiableMap(THashMap<String, PsiField>(fields.size).apply {
            for (field in fields) {
                putIfAbsent(field.name, field)
            }
        })
    }

    fun findFieldByName(name: String, checkBases: Boolean): PsiField? {
        return if (checkBases) {
            PsiClassImplUtil.findFieldByName(myClass, name, true)
        } else {
            fieldByNameCache.value[name]
        }
    }

    private val methodByNameCache = cache {
        val methods = this.methods.takeIf { it.isNotEmpty() } ?: return@cache emptyMap()
        Collections.unmodifiableMap(THashMap<String, Array<PsiMethod>>().apply {
            for ((key, list) in methods.groupByTo(HashMap()) { it.name }) {
                put(key, list.toTypedArray())
            }
        })
    }

    fun findMethodsByName(name: String, checkBases: Boolean): Array<PsiMethod> {
        return if (checkBases) {
            PsiClassImplUtil.findMethodsByName(myClass, name, true)
        } else {
            copy(methodByNameCache.value[name] ?: PsiMethod.EMPTY_ARRAY)
        }
    }

    private val innerClassByNameCache = cache {
        val classes = this.innerClasses.takeIf { it.isNotEmpty() } ?: return@cache emptyMap()

        Collections.unmodifiableMap(THashMap<String, PsiClass>().apply {
            for (psiClass in classes) {
                val name = psiClass.name
                if (name == null) {
                    Logger.getInstance(KotlinClassInnerStuffCache::class.java).error(psiClass)
                } else if (psiClass !is ExternallyDefinedPsiElement || !containsKey(name)) {
                    put(name, psiClass)
                }
            }
        })
    }

    fun findInnerClassByName(name: String, checkBases: Boolean): PsiClass? {
        return if (checkBases) {
            PsiClassImplUtil.findInnerByName(myClass, name, true)
        } else {
            innerClassByNameCache.value[name]
        }
    }

    private val valuesMethodCache = cache { EnumSyntheticMethod(myClass, EnumSyntheticMethod.Kind.VALUES) }

    private fun getValuesMethod(): PsiMethod? {
        if (myClass.isEnum && !isAnonymousClass() && !isClassNameSealed()) {
            return valuesMethodCache.value
        }

        return null
    }

    private val valueOfMethodCache = cache { EnumSyntheticMethod(myClass, EnumSyntheticMethod.Kind.VALUE_OF) }

    fun getValueOfMethod(): PsiMethod? {
        if (myClass.isEnum && !isAnonymousClass()) {
            return valueOfMethodCache.value
        }

        return null
    }

    private fun isClassNameSealed(): Boolean {
        return myClass.name == PsiKeyword.SEALED && PsiUtil.getLanguageLevel(myClass).toJavaVersion().feature >= 16
    }

    private fun isAnonymousClass(): Boolean {
        return myClass.name == null || myClass is PsiAnonymousClass
    }
}

private class EnumSyntheticMethod(
    private val enumClass: PsiClass,
    private val kind: Kind
) : LightElement(enumClass.manager, enumClass.language), PsiMethod, SyntheticElement {
    enum class Kind {
        VALUE_OF, VALUES
    }

    private val annotations = arrayOf(makeNotNullAnnotation(enumClass))

    override fun hasAnnotation(fqn: String): Boolean = annotations.any { it.hasQualifiedName(fqn) }
    override fun getAnnotation(fqn: String): PsiAnnotation? = annotations.firstOrNull { it.hasQualifiedName(fqn) }
    override fun getAnnotations(): Array<PsiAnnotation> = copy(annotations)

    private val returnType = run {
        val enumType = JavaPsiFacade.getElementFactory(project).createType(enumClass)
            .annotate { arrayOf(makeNotNullAnnotation(enumClass)) }

        when (kind) {
            Kind.VALUE_OF -> enumType
            Kind.VALUES -> enumType.createArrayType().annotate { arrayOf(makeNotNullAnnotation(enumClass)) }
        }
    }

    private val parameterList = LightParameterListBuilder(manager, language).apply {
        if (kind == Kind.VALUE_OF) {
            val stringType = PsiType.getJavaLangString(manager, GlobalSearchScope.allScope(project))
            val nameParameter = LightParameter("name", stringType, this, language, false).apply {
                setModifierList(PsiCompositeModifierList(manager, listOf(modifierList, NotNullModifierList(manager))))
            }
            addParameter(nameParameter)
        }
    }

    private val modifierList = object : LightModifierList(manager, language, PsiModifier.PUBLIC, PsiModifier.STATIC) {
        override fun getParent() = this@EnumSyntheticMethod
    }

    override fun getTextOffset(): Int = enumClass.textOffset
    override fun toString(): String = enumClass.toString()

    override fun equals(other: Any?): Boolean {
        return this === other || (other is EnumSyntheticMethod && enumClass == other.enumClass && kind == other.kind)
    }

    override fun hashCode() = Objects.hash(enumClass, kind)

    override fun isDeprecated(): Boolean = false
    override fun getDocComment(): PsiDocComment? = null

    @Suppress("UnstableApiUsage")
    override fun getContainingClass(): PsiClass = enumClass

    override fun getReturnType(): PsiType = returnType
    override fun getReturnTypeElement(): PsiTypeElement? = null
    override fun getParameterList(): PsiParameterList = parameterList

    override fun getThrowsList(): PsiReferenceList {
        return LightReferenceListBuilder(manager, language, PsiReferenceList.Role.THROWS_LIST).apply {
            if (kind == Kind.VALUE_OF) {
                addReference("java.lang.IllegalArgumentException")
            }
        }
    }

    override fun getBody(): PsiCodeBlock? = null
    override fun isConstructor(): Boolean = false
    override fun isVarArgs(): Boolean = false

    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature = MethodSignatureBackedByPsiMethod.create(this, substitutor)

    override fun getNameIdentifier(): PsiIdentifier = LightIdentifier(manager, name)

    override fun getName() = when (kind) {
        Kind.VALUE_OF -> "valueOf"
        Kind.VALUES -> "values"
    }

    override fun findSuperMethods(): Array<PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this)
    override fun findSuperMethods(checkAccess: Boolean): Array<PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess)
    override fun findSuperMethods(parentClass: PsiClass): Array<PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this, parentClass)

    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean): List<MethodSignatureBackedByPsiMethod> {
        return PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun findDeepestSuperMethod(): PsiMethod? = PsiSuperMethodImplUtil.findDeepestSuperMethod(this)

    override fun findDeepestSuperMethods(): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun getModifierList(): PsiModifierList = modifierList
    override fun hasModifierProperty(name: String) = name == PsiModifier.PUBLIC || name == PsiModifier.STATIC
    override fun setName(name: String): PsiElement = throw IncorrectOperationException()
    override fun getHierarchicalMethodSignature() = PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)
    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getContainingFile(): PsiFile = enumClass.containingFile
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    private companion object {
        private fun makeNotNullAnnotation(context: PsiClass): PsiAnnotation {
            return PsiElementFactory.getInstance(context.project).createAnnotationFromText("@" + NotNull::class.java.name, context)
        }
    }
}

private class NotNullModifierList(manager: PsiManager) : LightModifierList(manager) {
    private val annotation = PsiElementFactory.getInstance(project).createAnnotationFromText("@" + NotNull::class.java.name, context)
    override fun getAnnotations() = arrayOf(annotation)
}

private fun <T> copy(value: Array<T>): Array<T> {
    return if (value.isEmpty()) value else value.clone()
}