/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.js.translate.context

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.js.backend.ast.metadata.TypeCheck
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.naming.SuggestedName
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.backend.js.translate.intrinsic.functions.factories.ArrayFIF
import org.jetbrains.kotlin.backend.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.backend.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

import java.util.Arrays
import java.util.Collections

import org.jetbrains.kotlin.backend.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.backend.js.translate.utils.JsDescriptorUtils.getModuleName

/**
 * Encapsulates different types of constants and naming conventions.
 */
class Namer private constructor(rootScope: JsScope) {

    private val kotlinScope = JsObjectScope(rootScope, "Kotlin standard object")

    val callGetProperty = kotlin("callGetter")
        get() = field.deepCopy()

    val callSetProperty = kotlin("callSetter")
        get() = field.deepCopy()

    val isComparable: JsExpression
        get() = kotlin("isComparable")

    val isCharSequence: JsExpression
        get() = kotlin(IS_CHAR_SEQUENCE)

    val isArray: JsExpression
        get() = kotlin("isArray")

    fun kotlin(name: String): JsNameRef {
        return kotlin(kotlinScope.declareName(name))
    }

    fun isTypeOf(type: JsExpression): JsExpression {
        return invokeFunctionAndSetTypeCheckMetadata("isTypeOf", type, TypeCheck.TYPEOF)
    }

    fun isInstanceOf(type: JsExpression): JsExpression {
        return invokeFunctionAndSetTypeCheckMetadata("isInstanceOf", type, TypeCheck.INSTANCEOF)
    }

    fun orNull(callable: JsExpression): JsExpression {
        return invokeFunctionAndSetTypeCheckMetadata("orNull", callable, TypeCheck.OR_NULL)
    }

    fun andPredicate(a: JsExpression, b: JsExpression): JsExpression {
        return invokeFunctionAndSetTypeCheckMetadata("andPredicate", Arrays.asList(a, b), TypeCheck.AND_PREDICATE)
    }

    fun isPrimitiveArray(type: PrimitiveType): JsExpression {
        return kotlin("is" + type.arrayTypeName.asString())
    }

    private fun invokeFunctionAndSetTypeCheckMetadata(
        functionName: String,
        argument: JsExpression?,
        metadata: TypeCheck
    ): JsExpression {
        val arguments = if (argument != null) listOf(argument) else emptyList()
        return invokeFunctionAndSetTypeCheckMetadata(functionName, arguments, metadata)
    }

    private fun invokeFunctionAndSetTypeCheckMetadata(
        functionName: String,
        arguments: List<JsExpression>,
        metadata: TypeCheck
    ): JsExpression {
        val invocation = JsInvocation(kotlin(functionName))
        invocation.arguments.addAll(arguments)
        invocation.typeCheck = metadata
        invocation.sideEffects = SideEffectKind.PURE
        return invocation
    }

    companion object {
        @JvmField val KOTLIN_NAME = KotlinLanguage.NAME
        @JvmField val KOTLIN_LOWER_NAME = KOTLIN_NAME.toLowerCase()

        @JvmField val EQUALS_METHOD_NAME = getStableMangledNameForDescriptor(JsPlatform.builtIns.any, "equals")
        @JvmField val COMPARE_TO_METHOD_NAME = getStableMangledNameForDescriptor(JsPlatform.builtIns.comparable, "compareTo")
        @JvmField val LONG_FROM_NUMBER = "fromNumber"
        @JvmField val LONG_TO_NUMBER = "toNumber"
        @JvmField val LONG_FROM_INT = "fromInt"
        @JvmField val LONG_ZERO = "ZERO"
        @JvmField val LONG_ONE = "ONE"
        @JvmField val LONG_NEG_ONE = "NEG_ONE"
        @JvmField val PRIMITIVE_COMPARE_TO = "primitiveCompareTo"
        @JvmField val IS_CHAR = "isChar"
        @JvmField val IS_NUMBER = "isNumber"
        private val IS_CHAR_SEQUENCE = "isCharSequence"
        @JvmField val GET_KCLASS = "getKClass"
        @JvmField val GET_KCLASS_FROM_EXPRESSION = "getKClassFromExpression"

        @JvmField val CALLEE_NAME = "\$fun"

        @JvmField val CALL_FUNCTION = "call"
        private val APPLY_FUNCTION = "apply"

        @JvmField val OUTER_FIELD_NAME = "\$outer"

        @JvmField val delegatePrefix = "\$delegate"

        @JvmField val rootPackageName = "_"

        @JvmField val receiverParameterName = "\$receiver"
        @JvmField val ANOTHER_THIS_PARAMETER_NAME = "\$this"

        @JvmField val THROW_CLASS_CAST_EXCEPTION_FUN_NAME = "throwCCE"
        @JvmField val THROW_ILLEGAL_STATE_EXCEPTION_FUN_NAME = "throwISE"
        @JvmField val THROW_UNINITIALIZED_PROPERTY_ACCESS_EXCEPTION = "throwUPAE"
        @JvmField val NULL_CHECK_INTRINSIC_NAME = "ensureNotNull"
        @JvmStatic val prototypeName = "prototype"
        private val CAPTURED_VAR_FIELD = "v"

        @JvmField val IS_ARRAY_FUN_REF = JsNameRef("isArray", "Array")
        @JvmField val DEFINE_INLINE_FUNCTION = "defineInlineFunction"
        @JvmField val DEFAULT_PARAMETER_IMPLEMENTOR_SUFFIX = "\$default"

        private val JS_OBJECT = JsNameRef("Object")
        private val JS_OBJECT_CREATE_FUNCTION = JsNameRef("create", JS_OBJECT)

        @JvmField val LOCAL_MODULE_PREFIX = "\$module$"
        @JvmField val METADATA = "\$metadata$"
        @JvmField val METADATA_SUPERTYPES = "interfaces"
        @JvmField val METADATA_SIMPLE_NAME = "simpleName"
        @JvmField val METADATA_CLASS_KIND = "kind"
        @JvmField val CLASS_KIND_ENUM = "Kind"
        @JvmField val CLASS_KIND_CLASS = "CLASS"
        @JvmField val CLASS_KIND_INTERFACE = "INTERFACE"
        @JvmField val CLASS_KIND_OBJECT = "OBJECT"

        @JvmField val OBJECT_INSTANCE_VAR_SUFFIX = "_instance"
        @JvmField val OBJECT_INSTANCE_FUNCTION_SUFFIX = "_getInstance"

        @JvmField val ENUM_NAME_FIELD = "name$"
        @JvmField val ENUM_ORDINAL_FIELD = "ordinal$"

        @JvmField val IMPORTS_FOR_INLINE_PROPERTY = "$\$importsForInline$$"

        @JvmStatic
        fun getFunctionTag(functionDescriptor: CallableDescriptor, config: JsConfig): String {
            var functionDescriptor = functionDescriptor
            val intrinsicTag = ArrayFIF.getTag(functionDescriptor, config)
            if (intrinsicTag != null) return intrinsicTag

            functionDescriptor = JsDescriptorUtils.findRealInlineDeclaration(functionDescriptor) as CallableDescriptor
            val moduleName = getModuleName(functionDescriptor)
            val fqNameParent = DescriptorUtils.getFqName(functionDescriptor).parent()
            var qualifier: String? = null

            if (!fqNameParent.isRoot) {
                qualifier = fqNameParent.asString()
            }

            val suggestedName = NameSuggestion().suggest(functionDescriptor)
                    ?: error("Suggested name can be null only for module descriptors: " + functionDescriptor)
            val mangledName = suggestedName.names[0]
            return StringUtil.join(Arrays.asList<String>(moduleName, qualifier, mangledName), ".")
        }

        @JvmStatic
        fun getFunctionCallRef(functionExpression: JsExpression): JsNameRef {
            return pureFqn(CALL_FUNCTION, functionExpression)
        }

        @JvmStatic
        fun getFunctionApplyRef(functionExpression: JsExpression): JsNameRef {
            return pureFqn(APPLY_FUNCTION, functionExpression)
        }

        @JvmStatic
        fun createObjectWithPrototypeFrom(referenceToClass: JsExpression): JsInvocation {
            return JsInvocation(JS_OBJECT_CREATE_FUNCTION.deepCopy(), JsAstUtils.prototypeOf(referenceToClass))
        }

        @JvmStatic
        fun getCapturedVarAccessor(ref: JsExpression): JsNameRef {
            val result = JsNameRef(CAPTURED_VAR_FIELD, ref)
            result.sideEffects = SideEffectKind.DEPENDS_ON_STATE
            return result
        }

        @JvmStatic
        fun isInstanceSuggestedName(descriptor: TypeParameterDescriptor): String {
            return "is" + NameSuggestion.sanitizeName(descriptor.name.identifier)
        }

        @JvmStatic
        fun newInstance(rootScope: JsScope): Namer {
            return Namer(rootScope)
        }

        @JvmField
        val FUNCTION_CALLABLE_REF = "getCallableRef"
        @JvmField
        val PROPERTY_CALLABLE_REF = "getPropertyCallableRef"

        // TODO: get rid of this function
        private fun getStableMangledNameForDescriptor(descriptor: ClassDescriptor, functionName: String): String {
            val functions = descriptor.defaultType.memberScope.getContributedFunctions(
                Name.identifier(functionName), NoLookupLocation.FROM_BACKEND
            )
            assert(functions.size == 1) { "Can't select a single function: $functionName in $descriptor" }
            val suggested = NameSuggestion().suggest(functions.iterator().next())
                    ?: error("Suggested name for class members is always non-null: " + functions.iterator().next())
            return suggested.names[0]
        }

        @JvmStatic
        fun kotlin(name: JsName): JsNameRef {
            return pureFqn(name, kotlinObject())
        }

        @JvmStatic
        fun kotlinObject(): JsNameRef {
            return pureFqn(KOTLIN_NAME, null)
        }

        @JvmStatic
        fun isInstanceOf(instance: JsExpression, type: JsExpression): JsExpression {
            val result = JsInvocation(JsNameRef("isType", KOTLIN_NAME), instance, type)
            result.sideEffects = SideEffectKind.PURE
            return result
        }

        @JvmStatic
        fun getUndefinedExpression() = JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(0))

        @JvmStatic
        fun kotlinLong(): JsNameRef {
            return pureFqn("Long", kotlinObject())
        }

        @JvmStatic
        fun createSpecialFunction(specialFunction: SpecialFunction): JsExpression {
            return pureFqn(specialFunction.suggestedName, kotlinObject())
        }

        @JvmStatic
        fun suggestedModuleName(id: String): String {
            if (id.isEmpty()) {
                return "_"
            }

            val sb = StringBuilder(id.length)
            var c = id[0]
            if (Character.isJavaIdentifierStart(c)) {
                sb.append(c)
            } else {
                sb.append('_')
                if (Character.isJavaIdentifierPart(c)) {
                    sb.append(c)
                }
            }

            for (i in 1 until id.length) {
                c = id[i]
                sb.append(if (Character.isJavaIdentifierPart(c)) c else '_')
            }

            return sb.toString()
        }

        @JvmStatic
        fun imul(): JsNameRef {
            return pureFqn("imul", kotlinObject())
        }
    }
}
