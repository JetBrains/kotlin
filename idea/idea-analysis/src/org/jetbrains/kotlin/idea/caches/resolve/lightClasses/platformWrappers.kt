/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches.resolve.lightClasses


import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiSubstitutorImpl.createSubstitutor
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.*
import com.intellij.psi.impl.source.ClassInnerStuffCache
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.impl.source.PsiImmediateClassType
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import com.siyeh.ig.psiutils.TypeUtils
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.UserDataProperty

private val readOnlyQualifiedNamesToJavaClass = JavaToKotlinClassMap.mutabilityMappings.associateBy {
    (_, readOnly, _) ->
    readOnly.asSingleFqName()
}

private val mutableQualifiedNamesToJavaClass = JavaToKotlinClassMap.mutabilityMappings.associateBy {
    (_, _, mutable) ->
    mutable.asSingleFqName()
}

private val membersWithSpecializedSignature: Set<String> =
        BuiltinMethodsWithSpecialGenericSignature.ERASED_VALUE_PARAMETERS_SIGNATURES.mapTo(LinkedHashSet()) {
            val fqNameString = it.substringBefore('(').replace('/', '.')
            FqName(fqNameString).shortName().asString()
        }

private val javaGetterNameToKotlinGetterName: Map<String, String> = BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.map {
    (propertyFqName, javaGetterShortName) ->
    Pair(javaGetterShortName.asString(), JvmAbi.getterName(propertyFqName.shortName().asString()))
}.toMap()

fun platformMutabilityWrapper(fqName: FqName, findJavaClass: (String) -> PsiClass?): PsiClass? {
    readOnlyQualifiedNamesToJavaClass[fqName]?.let {
        (javaClass, kotlinReadOnly) ->
        val javaBaseClass = findJavaClass(javaClass.asSingleFqName().asString()) ?: return null
        return getOrCreateWrapper(javaBaseClass, kotlinReadOnly.asSingleFqName(), isMutable = false)
    }
    mutableQualifiedNamesToJavaClass[fqName]?.let {
        (javaClass, _, kotlinMutable) ->
        val javaBaseClass = findJavaClass(javaClass.asSingleFqName().asString()) ?: return null
        return getOrCreateWrapper(javaBaseClass, kotlinMutable.asSingleFqName(), isMutable = true)
    }
    return null
}

private fun getOrCreateWrapper(javaBaseClass: PsiClass, kotlinFqName: FqName, isMutable: Boolean): KtLightMutabilityPlatformWrapper {
    val userDataStorage = if (isMutable) javaBaseClass::mutableWrapper else javaBaseClass::readOnlyWrapper
    return userDataStorage.get() ?: KtLightMutabilityPlatformWrapper(javaBaseClass, kotlinFqName, isMutable).also { userDataStorage.set(it) }
}

private var PsiClass.readOnlyWrapper: KtLightMutabilityPlatformWrapper? by UserDataProperty(Key.create("READ_ONLY_WRAPPER"))
private var PsiClass.mutableWrapper: KtLightMutabilityPlatformWrapper? by UserDataProperty(Key.create("MUTABLE_WRAPPER"))

class KtLightMutabilityPlatformWrapper(
        private val javaBaseClass: PsiClass,
        private val kotlinInterfaceFqName: FqName,
        private val isMutable: Boolean
) : KtAbstractContainerWrapper(kotlinInterfaceFqName, javaBaseClass), PsiClass {
    private val _methods by lazyPub { calcMethods() }

    private fun calcMethods() = javaBaseClass.methods.flatMap { methodWrappers(it) }

    override fun getOwnMethods() = _methods

    private fun methodWrappers(method: PsiMethod): List<PsiMethod> {
        val methodName = method.name

        javaGetterNameToKotlinGetterName.get(methodName)?.let { kotlinName ->
            val finalBridgeForJava = method.finalBridge()
            val abstractKotlinGetter = method.wrap(name = kotlinName)
            return listOf(finalBridgeForJava, abstractKotlinGetter)
        }

        if (!method.isInKotlinInterface()) {
            // compiler generates stub override
            return listOf(method.openBridge())
        }

        return methodsWithSpecializedSignature(method)
    }

    private fun methodsWithSpecializedSignature(method: PsiMethod): List<PsiMethod> {
        val methodName = method.name

        if (methodName !in membersWithSpecializedSignature) return emptyList()

        if (javaBaseClass.qualifiedName == CommonClassNames.JAVA_UTIL_MAP) {
            val abstractKotlinVariantWithGeneric = javaUtilMapMethodWithSpecialSignature(method) ?: return emptyList()
            val finalBridgeWithObject = method.finalBridge()
            return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
        }

        if (methodName in BuiltinMethodsWithSpecialGenericSignature.ERASED_COLLECTION_PARAMETER_NAMES) {
            return emptyList()
        }

        if (methodName == "remove" && method.parameterList.parameters.singleOrNull()?.type == PsiType.INT) {
            // remove(int) -> abstract removeAt(int), final bridge remove(int)
            return listOf(method.finalBridge(), createRemoveAt(method))
        }


        val finalBridgeWithObject = method.finalBridge()
        val abstractKotlinVariantWithGeneric = method.wrap(substituteObjectWith = singleTypeParameterAsType())
        return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
    }

    private fun singleTypeParameterAsType() = typeParameters.single().asType()

    private fun createRemoveAt(baseMethod: PsiMethod): PsiMethod {
        return baseMethod.wrap(
                name = "removeAt",
                signature = MethodSignature(
                        parameterTypes = listOf(PsiType.INT),
                        returnType = singleTypeParameterAsType()
                )
        )
    }

    private fun PsiMethod.finalBridge() = wrap(makeFinal = true, hasImplementation = true)
    private fun PsiMethod.openBridge() = wrap(makeFinal = false, hasImplementation = true)

    private fun PsiMethod.wrap(
            makeFinal: Boolean = false,
            hasImplementation: Boolean = false,
            name: String = this.name,
            substituteObjectWith: PsiType? = null,
            signature: MethodSignature? = null
    ) = KtLightMethodWrapper(
            this@KtLightMutabilityPlatformWrapper, this@wrap,
            isFinal = makeFinal,
            name = name,
            substituteObjectWith = substituteObjectWith,
            providedSignature = signature,
            hasImplementation = hasImplementation
    )

    private fun javaUtilMapMethodWithSpecialSignature(method: PsiMethod): KtLightMethodWrapper? {
        val k = typeParameters[0].asType()
        val v = typeParameters[1].asType()

        val signature = when (method.name) {
            "get" -> MethodSignature(
                    parameterTypes = listOf(k),
                    returnType = v
            )
            "getOrDefault" -> MethodSignature(
                    parameterTypes = listOf(k, v),
                    returnType = v
            )
            "containsKey" -> MethodSignature(
                    parameterTypes = listOf(k),
                    returnType = PsiType.BOOLEAN
            )
            "containsValue" -> MethodSignature(
                    parameterTypes = listOf(v),
                    returnType = PsiType.BOOLEAN
            )
            "remove" ->
                when (method.parameterList.parametersCount) {
                    1 -> MethodSignature(
                            parameterTypes = listOf(k),
                            returnType = v
                    )
                    2 -> MethodSignature(
                            parameterTypes = listOf(k, v),
                            returnType = PsiType.BOOLEAN
                    )
                    else -> null
                }
            else -> null
        } ?: return null

        return method.wrap(signature = signature)
    }

    private fun PsiMethod.isInKotlinInterface(): Boolean {
        if (javaBaseClass.qualifiedName == CommonClassNames.JAVA_UTIL_MAP_ENTRY) {
            when (name) {
                "getValue", "getKey" -> return true
            }
        }

        val kotlinInterface = DefaultBuiltIns.Instance.getBuiltInClassByFqName(kotlinInterfaceFqName)
        val scope = kotlinInterface.unsubstitutedMemberScope

        val methodName = Name.identifier(name)
        return scope.getContributedFunctions(methodName, NoLookupLocation.FROM_IDE).isNotEmpty()
               || scope.getContributedVariables(methodName, NoLookupLocation.FROM_IDE).isNotEmpty()
    }

    override fun getContainingFile() = javaBaseClass.containingFile
}

private data class MethodSignature(val parameterTypes: List<PsiType>, val returnType: PsiType)

private class KtLightMethodWrapper(
        private val containingClass: KtAbstractContainerWrapper,
        private val baseMethod: PsiMethod,
        private val name: String,
        private val isFinal: Boolean,
        private val hasImplementation: Boolean,
        private val substituteObjectWith: PsiType?,
        private val providedSignature: MethodSignature?
) : PsiMethod, KtLightElementBase(containingClass) {

    init {
        if (!hasImplementation && isFinal) {
            error("Can't be final without an implementation")
        }
    }

    private fun substituteType(psiType: PsiType): PsiType {
        val substituted = containingClass.substitutor.substitute(psiType)
        if (TypeUtils.isJavaLangObject(substituted) && substituteObjectWith != null) {
            return substituteObjectWith
        }
        else {
            return substituted
        }
    }

    override fun getPresentation() = baseMethod.presentation

    override val kotlinOrigin get() = null

    override fun hasModifierProperty(name: String) =
            when (name) {
                PsiModifier.DEFAULT -> hasImplementation
                PsiModifier.ABSTRACT -> !hasImplementation
                PsiModifier.FINAL -> isFinal
                else -> baseMethod.hasModifierProperty(name)
            }

    override fun getParameterList(): PsiParameterList {
        return LightParameterListBuilder(manager, KotlinLanguage.INSTANCE).apply {
            baseMethod.parameterList.parameters.forEachIndexed { index, paramFromJava ->
                val type = providedSignature?.parameterTypes?.get(index) ?: substituteType(paramFromJava.type)
                addParameter(
                        LightParameter(paramFromJava.name ?: "p$index", type,
                                       this@KtLightMethodWrapper, KotlinLanguage.INSTANCE, paramFromJava.isVarArgs)
                )
            }
        }
    }

    override fun getName() = name
    override fun getReturnType() = providedSignature?.returnType ?: baseMethod.returnType?.let { substituteType(it) }

    override fun getTypeParameters() = baseMethod.typeParameters
    override fun getTypeParameterList() = baseMethod.typeParameterList

    override fun findSuperMethods(checkAccess: Boolean) = PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess)
    override fun findSuperMethods(parentClass: PsiClass) = PsiSuperMethodImplUtil.findSuperMethods(this, parentClass)
    override fun findSuperMethods() = PsiSuperMethodImplUtil.findSuperMethods(this)
    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean) = PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess)
    @Suppress("OverridingDeprecatedMember") override fun findDeepestSuperMethod() = PsiSuperMethodImplUtil.findDeepestSuperMethod(this)
    override fun findDeepestSuperMethods() = PsiSuperMethodImplUtil.findDeepestSuperMethods(this)
    override fun getHierarchicalMethodSignature() = PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)
    override fun getSignature(substitutor: PsiSubstitutor) = MethodSignatureBackedByPsiMethod.create(this, substitutor)
    override fun getReturnTypeElement(): PsiTypeElement? = null
    override fun getContainingClass() = containingClass
    override fun getThrowsList() = baseMethod.throwsList
    override fun hasTypeParameters() = baseMethod.hasTypeParameters()
    override fun isVarArgs() = baseMethod.isVarArgs
    override fun isConstructor() = false
    private val identifier by lazyPub { LightIdentifier(manager, name) }
    override fun getNameIdentifier() = identifier
    override fun getDocComment() = baseMethod.docComment
    override fun getModifierList() = baseMethod.modifierList
    override fun getBody() = null
    override fun isDeprecated() = baseMethod.isDeprecated
    override fun setName(name: String) = cannotModify()

    override fun toString(): String {
        return "$javaClass:$name${parameterList.parameters.map { it.type }.joinToString(prefix = "(", postfix = ")", separator = ", ")}"
    }
}


abstract class KtAbstractContainerWrapper(internal val fqName: FqName, private val superInterface: PsiClass)
    : LightElement(superInterface.manager, KotlinLanguage.INSTANCE), PsiExtensibleClass {

    private val memberCache = ClassInnerStuffCache(this)

    private val superClassTypeParametersToMyTypeParameters: Map<PsiTypeParameter, PsiTypeParameter>
            = superInterface.typeParameters
            .mapIndexed { index, supersParameter ->
                supersParameter to LightTypeParameterBuilder(supersParameter.name ?: "T$index", this, index)
            }
            .toMap()

    internal val substitutor = createSubstitutor(superClassTypeParametersToMyTypeParameters.mapValues {
        it.value.asType()
    })

    override fun getSupers() = arrayOf(superInterface)

    override fun getQualifiedName() = fqName.asString()

    override fun toString() = "$javaClass:$name"

    override fun hasModifierProperty(name: String) = name == PsiModifier.PUBLIC || name == PsiModifier.ABSTRACT

    private val _typeParameterList by lazyPub {
        LightTypeParameterListBuilder(manager, KotlinLanguage.INSTANCE).apply {
            superClassTypeParametersToMyTypeParameters.values.forEach { addParameter(it) }
        }
    }

    override fun getTypeParameterList() = _typeParameterList

    private val identifier by lazyPub { LightIdentifier(manager, name) }
    override fun getNameIdentifier() = identifier

    override fun getName() = fqName.shortName().asString()

    private val _implementsList by lazyPub {
        LightReferenceListBuilder(manager, PsiReferenceList.Role.IMPLEMENTS_LIST).apply {
            addReference(superInterface)
        }
    }

    override fun getImplementsList() = _implementsList

    override fun getSuperTypes() = arrayOf(PsiImmediateClassType(superInterface, substitutor))

    override fun getMethods() = memberCache.methods

    override fun getTypeParameters() = superClassTypeParametersToMyTypeParameters.values.toTypedArray()

    override fun getInterfaces() = arrayOf(superInterface)

    override fun getInitializers() = PsiClassInitializer.EMPTY_ARRAY

    override fun getContainingClass() = null

    override fun getFields() = PsiField.EMPTY_ARRAY

    override fun isInterface() = true
    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean) = InheritanceImplUtil.isInheritor(this, baseClass, checkDeep)
    override fun getOwnInnerClasses() = emptyList<PsiClass>()
    override fun getSuperClass() = null
    override fun findInnerClassByName(name: String?, checkBases: Boolean) = null
    override fun getExtendsListTypes() = PsiClassType.EMPTY_ARRAY
    override fun isInheritorDeep(baseClass: PsiClass, classToByPass: PsiClass?) = InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass)
    override fun isAnnotationType() = false
    override fun findMethodsAndTheirSubstitutorsByName(name: String?, checkBases: Boolean)
            = PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases)

    override fun getInnerClasses() = PsiClass.EMPTY_ARRAY
    override fun findMethodBySignature(patternMethod: PsiMethod, checkBases: Boolean)
            = PsiClassImplUtil.findMethodBySignature(this, patternMethod, checkBases)

    override fun findFieldByName(name: String?, checkBases: Boolean) = null
    override fun getAllFields() = PsiClassImplUtil.getAllFields(this)
    override fun getAllInnerClasses() = PsiClassImplUtil.getAllInnerClasses(this)
    override fun findMethodsByName(name: String?, checkBases: Boolean) = memberCache.findMethodsByName(name, checkBases)
    override fun getAllMethods() = PsiClassImplUtil.getAllMethods(this)
    override fun getOwnFields() = emptyList<PsiField>()
    override fun getAllMethodsAndTheirSubstitutors() =
            PsiClassImplUtil.getAllWithSubstitutorsByMap<PsiMethod>(this, PsiClassImplUtil.MemberType.METHOD)

    override fun hasTypeParameters() = true
    override fun getRBrace() = null
    override fun getLBrace() = null
    override fun getVisibleSignatures() = PsiSuperMethodImplUtil.getVisibleSignatures(this)
    override fun getExtendsList() = null
    override fun getDocComment() = null
    override fun isEnum() = false
    private val _modifierList by lazyPub { LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC) }
    override fun getModifierList() = _modifierList
    override fun getScope() = superInterface.scope
    override fun getImplementsListTypes() = superTypes
    override fun getConstructors() = PsiMethod.EMPTY_ARRAY
    override fun isDeprecated() = false
    override fun setName(name: String) = cannotModify()
    override fun findMethodsBySignature(patternMethod: PsiMethod, checkBases: Boolean)
            = PsiClassImplUtil.findMethodsBySignature(this, patternMethod, checkBases)
}

private fun PsiTypeParameter.asType() = PsiImmediateClassType(this, PsiSubstitutor.EMPTY)