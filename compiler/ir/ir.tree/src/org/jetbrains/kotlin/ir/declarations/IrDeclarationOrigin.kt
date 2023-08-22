/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.GeneratedDeclarationKey

interface IrDeclarationOrigin {
    object DEFINED : IrDeclarationOriginImpl("DEFINED")
    object FAKE_OVERRIDE : IrDeclarationOriginImpl("FAKE_OVERRIDE")
    object FOR_LOOP_ITERATOR : IrDeclarationOriginImpl("FOR_LOOP_ITERATOR")
    object FOR_LOOP_VARIABLE : IrDeclarationOriginImpl("FOR_LOOP_VARIABLE")
    object FOR_LOOP_IMPLICIT_VARIABLE : IrDeclarationOriginImpl("FOR_LOOP_IMPLICIT_VARIABLE")
    object PROPERTY_BACKING_FIELD : IrDeclarationOriginImpl("PROPERTY_BACKING_FIELD")
    object DEFAULT_PROPERTY_ACCESSOR : IrDeclarationOriginImpl("DEFAULT_PROPERTY_ACCESSOR")
    object DELEGATE : IrDeclarationOriginImpl("DELEGATE", isSynthetic = true)
    object PROPERTY_DELEGATE : IrDeclarationOriginImpl("PROPERTY_DELEGATE")
    object DELEGATED_PROPERTY_ACCESSOR : IrDeclarationOriginImpl("DELEGATED_PROPERTY_ACCESSOR")
    object DELEGATED_MEMBER : IrDeclarationOriginImpl("DELEGATED_MEMBER")
    object ENUM_CLASS_SPECIAL_MEMBER : IrDeclarationOriginImpl("ENUM_CLASS_SPECIAL_MEMBER")
    object FUNCTION_FOR_DEFAULT_PARAMETER : IrDeclarationOriginImpl("FUNCTION_FOR_DEFAULT_PARAMETER", isSynthetic = true)
    object MASK_FOR_DEFAULT_FUNCTION : IrDeclarationOriginImpl("MASK_FOR_DEFAULT_FUNCTION", isSynthetic = true)
    object DEFAULT_CONSTRUCTOR_MARKER : IrDeclarationOriginImpl("DEFAULT_CONSTRUCTOR_MARKER", isSynthetic = true)
    object METHOD_HANDLER_IN_DEFAULT_FUNCTION : IrDeclarationOriginImpl("METHOD_HANDLER_IN_DEFAULT_FUNCTION", isSynthetic = true)
    object MOVED_DISPATCH_RECEIVER : IrDeclarationOriginImpl("MOVED_DISPATCH_RECEIVER")
    object MOVED_EXTENSION_RECEIVER : IrDeclarationOriginImpl("MOVED_EXTENSION_RECEIVER")
    object MOVED_CONTEXT_RECEIVER : IrDeclarationOriginImpl("MOVED_CONTEXT_RECEIVER")

    object FILE_CLASS : IrDeclarationOriginImpl("FILE_CLASS")
    object SYNTHETIC_FILE_CLASS : IrDeclarationOriginImpl("SYNTHETIC_FILE_CLASS", isSynthetic = true)
    object JVM_MULTIFILE_CLASS : IrDeclarationOriginImpl("JVM_MULTIFILE_CLASS")
    object ERROR_CLASS : IrDeclarationOriginImpl("ERROR_CLASS")

    object SCRIPT_CLASS : IrDeclarationOriginImpl("SCRIPT_CLASS")
    object SCRIPT_THIS_RECEIVER : IrDeclarationOriginImpl("SCRIPT_THIS_RECEIVER")
    object SCRIPT_STATEMENT : IrDeclarationOriginImpl("SCRIPT_STATEMENT")
    object SCRIPT_EARLIER_SCRIPTS : IrDeclarationOriginImpl("SCRIPT_EARLIER_SCRIPTS")
    object SCRIPT_CALL_PARAMETER : IrDeclarationOriginImpl("SCRIPT_CALL_PARAMETER")
    object SCRIPT_IMPLICIT_RECEIVER : IrDeclarationOriginImpl("SCRIPT_IMPLICIT_RECEIVER")
    object SCRIPT_PROVIDED_PROPERTY : IrDeclarationOriginImpl("SCRIPT_PROVIDED_PROPERTY")
    object SCRIPT_RESULT_PROPERTY : IrDeclarationOriginImpl("SCRIPT_RESULT_PROPERTY")
    object GENERATED_DATA_CLASS_MEMBER : IrDeclarationOriginImpl("GENERATED_DATA_CLASS_MEMBER")
    object GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER : IrDeclarationOriginImpl("GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER")
    object GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER : IrDeclarationOriginImpl("GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER")
    object LOCAL_FUNCTION : IrDeclarationOriginImpl("LOCAL_FUNCTION")
    object LOCAL_FUNCTION_FOR_LAMBDA : IrDeclarationOriginImpl("LOCAL_FUNCTION_FOR_LAMBDA")
    object CATCH_PARAMETER : IrDeclarationOriginImpl("CATCH_PARAMETER")
    object UNDERSCORE_PARAMETER : IrDeclarationOriginImpl("UNDERSCORE_PARAMETER")
    object DESTRUCTURED_OBJECT_PARAMETER : IrDeclarationOriginImpl("DESTRUCTURED_OBJECT_PARAMETER")
    object INSTANCE_RECEIVER : IrDeclarationOriginImpl("INSTANCE_RECEIVER")
    object PRIMARY_CONSTRUCTOR_PARAMETER : IrDeclarationOriginImpl("PRIMARY_CONSTRUCTOR_PARAMETER")
    object IR_TEMPORARY_VARIABLE : IrDeclarationOriginImpl("IR_TEMPORARY_VARIABLE")
    object IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER : IrDeclarationOriginImpl("IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER")
    object IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER : IrDeclarationOriginImpl("IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER")
    object IR_EXTERNAL_DECLARATION_STUB : IrDeclarationOriginImpl("IR_EXTERNAL_DECLARATION_STUB")
    object IR_EXTERNAL_JAVA_DECLARATION_STUB : IrDeclarationOriginImpl("IR_EXTERNAL_JAVA_DECLARATION_STUB")
    object IR_BUILTINS_STUB : IrDeclarationOriginImpl("IR_BUILTINS_STUB")
    object BRIDGE : IrDeclarationOriginImpl("BRIDGE", isSynthetic = true)
    object BRIDGE_SPECIAL : IrDeclarationOriginImpl("BRIDGE_SPECIAL")
    object GENERATED_SETTER_GETTER : IrDeclarationOriginImpl("GENERATED_SETTER_GETTER", isSynthetic = true)

    object FIELD_FOR_ENUM_ENTRY : IrDeclarationOriginImpl("FIELD_FOR_ENUM_ENTRY")
    object SYNTHETIC_HELPER_FOR_ENUM_VALUES : IrDeclarationOriginImpl("SYNTHETIC_HELPER_FOR_ENUM_VALUES", isSynthetic = true)
    object SYNTHETIC_HELPER_FOR_ENUM_ENTRIES : IrDeclarationOriginImpl("SYNTHETIC_HELPER_FOR_ENUM_ENTRIES", isSynthetic = true)
    object FIELD_FOR_ENUM_VALUES : IrDeclarationOriginImpl("FIELD_FOR_ENUM_VALUES", isSynthetic = true)
    object FIELD_FOR_ENUM_ENTRIES: IrDeclarationOriginImpl("FIELD_FOR_ENUM_ENTRIES", isSynthetic = true)
    object PROPERTY_FOR_ENUM_ENTRIES: IrDeclarationOriginImpl("PROPERTY_FOR_ENUM_ENTRIES", isSynthetic = false)
    object FIELD_FOR_OBJECT_INSTANCE : IrDeclarationOriginImpl("FIELD_FOR_OBJECT_INSTANCE")
    object FIELD_FOR_CLASS_CONTEXT_RECEIVER : IrDeclarationOriginImpl("FIELD_FOR_CLASS_CONTEXT_RECEIVER", isSynthetic = true)

    object ADAPTER_FOR_CALLABLE_REFERENCE : IrDeclarationOriginImpl("ADAPTER_FOR_CALLABLE_REFERENCE", isSynthetic = true)
    object ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE : IrDeclarationOriginImpl("ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE")
    object ADAPTER_FOR_SUSPEND_CONVERSION : IrDeclarationOriginImpl("ADAPTER_FOR_SUSPEND_CONVERSION", isSynthetic = true)
    object ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION : IrDeclarationOriginImpl("ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION")
    object ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR : IrDeclarationOriginImpl("ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR", isSynthetic = true)

    object GENERATED_SAM_IMPLEMENTATION : IrDeclarationOriginImpl("GENERATED_SAM_IMPLEMENTATION")
    object SYNTHETIC_GENERATED_SAM_IMPLEMENTATION : IrDeclarationOriginImpl("SYNTHETIC_GENERATED_SAM_IMPLEMENTATION", isSynthetic = true)

    object SYNTHETIC_JAVA_PROPERTY_DELEGATE : IrDeclarationOriginImpl("SYNTHETIC_JAVA_PROPERTY_DELEGATE", isSynthetic = true)

    object FIELD_FOR_OUTER_THIS : IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS", isSynthetic = true)
    object CONTINUATION : IrDeclarationOriginImpl("CONTINUATION", isSynthetic = true)
    object LOWERED_SUSPEND_FUNCTION : IrDeclarationOriginImpl("LOWERED_SUSPEND_FUNCTION", isSynthetic = true)

    object SHARED_VARIABLE_IN_EVALUATOR_FRAGMENT : IrDeclarationOriginImpl("SHARED_VARIABLE_IN_EVALUATOR_FRAGMENT", isSynthetic = true)

    /**
     * [pluginKey] may be null if declaration with this origin was deserialized from klib
     */
    class GeneratedByPlugin private constructor(val pluginId: String, val pluginKey: GeneratedDeclarationKey?) : IrDeclarationOrigin {
        constructor(pluginKey: GeneratedDeclarationKey) : this(pluginKey::class.qualifiedName!!, pluginKey)

        companion object {
            fun fromSerializedString(name: String): GeneratedByPlugin? {
                val pluginId = name.removeSurrounding("GENERATED[", "]").takeIf { it != name } ?: return null
                return GeneratedByPlugin(pluginId, pluginKey = null)
            }
        }

        override val name: String
            get() = "GENERATED[${pluginId}]"

        override fun toString(): String {
            return name
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is GeneratedByPlugin) return false
            return pluginKey == other.pluginKey
        }

        override fun hashCode(): Int {
            return pluginKey.hashCode()
        }
    }

    val name: String
    val isSynthetic: Boolean get() = false
}

abstract class IrDeclarationOriginImpl(
    override val name: String,
    override val isSynthetic: Boolean = false,
    // Comparison of origins is a hot spot. We assign integer identifiers to optimize it
    private val id: Int = allOriginNames[name] ?: 0,
) : IrDeclarationOrigin {

    private companion object {
        val allOriginNames = HashMap<String, Int>()
        private var counter = 1

        private fun addOriginName(name: String) {
            allOriginNames[name] = counter++
        }

        init {
            addOriginName("ATOMICFU_GENERATED_CLASS")
            addOriginName("ATOMICFU_GENERATED_FUNCTION")
            addOriginName("ATOMICFU_GENERATED_FIELD")
            addOriginName("ATOMICFU_GENERATED_PROPERTY")
            addOriginName("ATOMICFU_GENERATED_PROPERTY_ACCESSOR")
            addOriginName("KOTLINX_SERIALIZATION")
            addOriginName("FORWARD_DECLARATION_ORIGIN")
            addOriginName("OBJC_BLOCK_FUNCTION_IMPL")
            addOriginName("DECLARATION_ORIGIN_FUNCTION_CLASS")
            addOriginName("INLINE_CLASS_SPECIAL_FUNCTION")
            addOriginName("INTERNAL_ABI")
            addOriginName("OVERRIDING_INITIALIZER_BY_CONSTRUCTOR")
            addOriginName("STATIC_GLOBAL_INITIALIZER")
            addOriginName("STATIC_THREAD_LOCAL_INITIALIZER")
            addOriginName("STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER")
            addOriginName("ENUM")
            addOriginName("TEST_SUITE_CLASS")
            addOriginName("TEST_SUITE_GENERATED_MEMBER")
            addOriginName("ANONYMOUS_INITIALIZER")
            addOriginName("VOLATILE")
            addOriginName("KPROPERTIES_FOR_DELEGATION")
            addOriginName("FUNCTION_REFERENCE_IMPL")
            addOriginName("ENTRY_POINT")
            addOriginName("BUILTIN_CLASS_CONSTRUCTOR")
            addOriginName("BUILTIN_CLASS_METHOD")
            addOriginName("OPTIMISED_WHEN_SUBJECT")
            addOriginName("TEMP_CLASS_FOR_INTERPRETER")
            addOriginName("TEMP_FUNCTION_FOR_INTERPRETER")
            addOriginName("COROUTINE_IMPL")
            addOriginName("BOUND_VALUE_PARAMETER")
            addOriginName("BOUND_RECEIVER_PARAMETER")
            addOriginName("FIELD_FOR_CAPTURED_VALUE")
            addOriginName("FIELD_FOR_CROSSINLINE_CAPTURED_VALUE")
            addOriginName("ANNOTATION_IMPLEMENTATION")
            addOriginName("arrayConstructorWrapper")
            addOriginName("SYNTHESIZED_DECLARATION")
            addOriginName("JS_INTRINSICS_STUB")
            addOriginName("JS_CLOSURE_BOX_CLASS_DECLARATION")
            addOriginName("BRIDGE_WITH_STABLE_NAME")
            addOriginName("BRIDGE_WITHOUT_STABLE_NAME")
            addOriginName("BRIDGE_PROPERTY_ACCESSOR")
            addOriginName("OBJECT_GET_INSTANCE_FUNCTION")
            addOriginName("JS_SHADOWED_EXPORT")
            addOriginName("JS_SUPER_CONTEXT_PARAMETER")
            addOriginName("JS_SHADOWED_DEFAULT_PARAMETER")
            addOriginName("ENUM_GET_INSTANCE_FUNCTION")
            addOriginName("ES6_BOX_PARAMETER")
            addOriginName("LAMBDA_IMPL")
            addOriginName("GENERATED_MEMBER_IN_CALLABLE_REFERENCE")
            addOriginName("ES6_CONSTRUCTOR_REPLACEMENT")
            addOriginName("ES6_SYNTHETIC_EXPORT_CONSTRUCTOR")
            addOriginName("ES6_PRIMARY_CONSTRUCTOR_REPLACEMENT")
            addOriginName("ES6_INIT_FUNCTION")
            addOriginName("ES6_DELEGATING_CONSTRUCTOR_CALL_REPLACEMENT")
            addOriginName("STATIC_THIS_PARAMETER")
            addOriginName("SYNTHETIC_PRIMARY_CONSTRUCTOR")
            addOriginName("OUTLINED_JS_CODE")
            addOriginName("SCRIPT_FUNCTION")
            addOriginName("PROPERTY_REFERNCE_FACTORY")
            addOriginName("ES6_THROWABLE_CONSTRUCTOR_SLOT")
            addOriginName("FUNCTION_INTERFACE_CLASS")
            addOriginName("FUNCTION_INTERFACE_MEMBER")
            addOriginName("FACTORY_ORIGIN")
            addOriginName("OPERATOR")
            addOriginName("SCRIPT")
            addOriginName("SCRIPT_K2")
            addOriginName("DEFINED")
            addOriginName("FAKE_OVERRIDE")
            addOriginName("FOR_LOOP_ITERATOR")
            addOriginName("FOR_LOOP_VARIABLE")
            addOriginName("FOR_LOOP_IMPLICIT_VARIABLE")
            addOriginName("PROPERTY_BACKING_FIELD")
            addOriginName("DEFAULT_PROPERTY_ACCESSOR")
            addOriginName("DELEGATE")
            addOriginName("PROPERTY_DELEGATE")
            addOriginName("DELEGATED_PROPERTY_ACCESSOR")
            addOriginName("DELEGATED_MEMBER")
            addOriginName("ENUM_CLASS_SPECIAL_MEMBER")
            addOriginName("FUNCTION_FOR_DEFAULT_PARAMETER")
            addOriginName("MASK_FOR_DEFAULT_FUNCTION")
            addOriginName("DEFAULT_CONSTRUCTOR_MARKER")
            addOriginName("METHOD_HANDLER_IN_DEFAULT_FUNCTION")
            addOriginName("MOVED_DISPATCH_RECEIVER")
            addOriginName("MOVED_EXTENSION_RECEIVER")
            addOriginName("MOVED_CONTEXT_RECEIVER")
            addOriginName("FILE_CLASS")
            addOriginName("SYNTHETIC_FILE_CLASS")
            addOriginName("JVM_MULTIFILE_CLASS")
            addOriginName("ERROR_CLASS")
            addOriginName("SCRIPT_CLASS")
            addOriginName("SCRIPT_THIS_RECEIVER")
            addOriginName("SCRIPT_STATEMENT")
            addOriginName("SCRIPT_EARLIER_SCRIPTS")
            addOriginName("SCRIPT_CALL_PARAMETER")
            addOriginName("SCRIPT_IMPLICIT_RECEIVER")
            addOriginName("SCRIPT_PROVIDED_PROPERTY")
            addOriginName("SCRIPT_RESULT_PROPERTY")
            addOriginName("GENERATED_DATA_CLASS_MEMBER")
            addOriginName("GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER")
            addOriginName("GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER")
            addOriginName("LOCAL_FUNCTION")
            addOriginName("LOCAL_FUNCTION_FOR_LAMBDA")
            addOriginName("CATCH_PARAMETER")
            addOriginName("UNDERSCORE_PARAMETER")
            addOriginName("DESTRUCTURED_OBJECT_PARAMETER")
            addOriginName("INSTANCE_RECEIVER")
            addOriginName("PRIMARY_CONSTRUCTOR_PARAMETER")
            addOriginName("IR_TEMPORARY_VARIABLE")
            addOriginName("IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER")
            addOriginName("IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER")
            addOriginName("IR_EXTERNAL_DECLARATION_STUB")
            addOriginName("IR_EXTERNAL_JAVA_DECLARATION_STUB")
            addOriginName("IR_BUILTINS_STUB")
            addOriginName("BRIDGE")
            addOriginName("BRIDGE_SPECIAL")
            addOriginName("GENERATED_SETTER_GETTER")
            addOriginName("FIELD_FOR_ENUM_ENTRY")
            addOriginName("SYNTHETIC_HELPER_FOR_ENUM_VALUES")
            addOriginName("SYNTHETIC_HELPER_FOR_ENUM_ENTRIES")
            addOriginName("FIELD_FOR_ENUM_VALUES")
            addOriginName("FIELD_FOR_ENUM_ENTRIES")
            addOriginName("PROPERTY_FOR_ENUM_ENTRIES")
            addOriginName("FIELD_FOR_OBJECT_INSTANCE")
            addOriginName("FIELD_FOR_CLASS_CONTEXT_RECEIVER")
            addOriginName("ADAPTER_FOR_CALLABLE_REFERENCE")
            addOriginName("ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE")
            addOriginName("ADAPTER_FOR_SUSPEND_CONVERSION")
            addOriginName("ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION")
            addOriginName("ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR")
            addOriginName("GENERATED_SAM_IMPLEMENTATION")
            addOriginName("SYNTHETIC_GENERATED_SAM_IMPLEMENTATION")
            addOriginName("SYNTHETIC_JAVA_PROPERTY_DELEGATE")
            addOriginName("FIELD_FOR_OUTER_THIS")
            addOriginName("CONTINUATION")
            addOriginName("LOWERED_SUSPEND_FUNCTION")
            addOriginName("SHARED_VARIABLE_IN_EVALUATOR_FRAGMENT")
            addOriginName("CLASS_STATIC_INITIALIZER")
            addOriginName("DEFAULT_IMPLS")
            addOriginName("SUPER_INTERFACE_METHOD_BRIDGE")
            addOriginName("SYNTHETIC_ACCESSOR")
            addOriginName("SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR")
            addOriginName("SYNTHETIC_MARKER_PARAMETER")
            addOriginName("TO_ARRAY")
            addOriginName("JVM_STATIC_WRAPPER")
            addOriginName("JVM_OVERLOADS_WRAPPER")
            addOriginName("SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS")
            addOriginName("GENERATED_PROPERTY_REFERENCE")
            addOriginName("ENUM_MAPPINGS_FOR_WHEN")
            addOriginName("ENUM_MAPPINGS_FOR_ENTRIES")
            addOriginName("SYNTHETIC_INLINE_CLASS_MEMBER")
            addOriginName("SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER")
            addOriginName("INLINE_CLASS_GENERATED_IMPL_METHOD")
            addOriginName("MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD")
            addOriginName("STATIC_INLINE_CLASS_REPLACEMENT")
            addOriginName("STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT")
            addOriginName("STATIC_INLINE_CLASS_CONSTRUCTOR")
            addOriginName("STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR")
            addOriginName("GENERATED_ASSERTION_ENABLED_FIELD")
            addOriginName("GENERATED_MULTI_FIELD_VALUE_CLASS_PARAMETER")
            addOriginName("TEMPORARY_MULTI_FIELD_VALUE_CLASS_PARAMETER")
            addOriginName("TEMPORARY_MULTI_FIELD_VALUE_CLASS_VARIABLE")
            addOriginName("MULTI_FIELD_VALUE_CLASS_REPRESENTATION_VARIABLE")
            addOriginName("GENERATED_EXTENDED_MAIN")
            addOriginName("SUSPEND_IMPL_STATIC_FUNCTION")
            addOriginName("INTERFACE_COMPANION_PRIVATE_INSTANCE")
            addOriginName("POLYMORPHIC_SIGNATURE_INSTANTIATION")
            addOriginName("ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER")
            addOriginName("OBJECT_SUPER_CONSTRUCTOR_PARAMETER")
            addOriginName("CONTINUATION_CLASS")
            addOriginName("SUSPEND_LAMBDA")
            addOriginName("FOR_INLINE_TEMPLATE")
            addOriginName("FOR_INLINE_TEMPLATE_CROSSINLINE")
            addOriginName("CONTINUATION_CLASS_RESULT_FIELD")
            addOriginName("COMPANION_PROPERTY_BACKING_FIELD")
            addOriginName("FIELD_FOR_STATIC_CALLABLE_REFERENCE_INSTANCE")
            addOriginName("ABSTRACT_BRIDGE_STUB")
            addOriginName("INVOKEDYNAMIC_CALL_TARGET")
            addOriginName("INLINE_LAMBDA")
            addOriginName("PROXY_FUN_FOR_METAFACTORY")
            addOriginName("SYNTHETIC_PROXY_FUN_FOR_METAFACTORY")
            addOriginName("DESERIALIZE_LAMBDA_FUN")
        }
    }

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (other !is IrDeclarationOriginImpl) return false
        val thisId = id
        val otherId = other.id
        return if (thisId == 0 && otherId == 0) name == other.name else thisId == otherId
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
