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
    override val isSynthetic: Boolean = false
) : IrDeclarationOrigin {
    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IrDeclarationOriginImpl) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
