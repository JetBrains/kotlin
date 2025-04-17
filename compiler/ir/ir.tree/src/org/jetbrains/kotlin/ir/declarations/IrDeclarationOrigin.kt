/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.GeneratedDeclarationKey
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface IrDeclarationOrigin {
    val name: String
    val isSynthetic: Boolean
        get() = false

    companion object {
        val DEFINED by IrDeclarationOriginImpl
        val FAKE_OVERRIDE by IrDeclarationOriginImpl
        val FOR_LOOP_ITERATOR by IrDeclarationOriginImpl
        val FOR_LOOP_VARIABLE by IrDeclarationOriginImpl
        val FOR_LOOP_IMPLICIT_VARIABLE by IrDeclarationOriginImpl
        val PROPERTY_BACKING_FIELD by IrDeclarationOriginImpl
        val DEFAULT_PROPERTY_ACCESSOR by IrDeclarationOriginImpl
        val DELEGATE by IrDeclarationOriginImpl.Synthetic
        val PROPERTY_DELEGATE by IrDeclarationOriginImpl
        val DELEGATED_PROPERTY_ACCESSOR by IrDeclarationOriginImpl
        val DELEGATED_MEMBER by IrDeclarationOriginImpl
        val ENUM_CLASS_SPECIAL_MEMBER by IrDeclarationOriginImpl
        val FUNCTION_FOR_DEFAULT_PARAMETER by IrDeclarationOriginImpl.Synthetic
        val MASK_FOR_DEFAULT_FUNCTION by IrDeclarationOriginImpl.Synthetic
        val DEFAULT_CONSTRUCTOR_MARKER by IrDeclarationOriginImpl.Synthetic
        val METHOD_HANDLER_IN_DEFAULT_FUNCTION by IrDeclarationOriginImpl.Synthetic
        val MOVED_DISPATCH_RECEIVER by IrDeclarationOriginImpl
        val MOVED_EXTENSION_RECEIVER by IrDeclarationOriginImpl
        val EXTENSION_RECEIVER_WITH_FIXED_NAME by IrDeclarationOriginImpl
        val MOVED_CONTEXT_RECEIVER by IrDeclarationOriginImpl

        val FILE_CLASS by IrDeclarationOriginImpl
        val SYNTHETIC_FILE_CLASS by IrDeclarationOriginImpl.Synthetic
        val JVM_MULTIFILE_CLASS by IrDeclarationOriginImpl
        val ERROR_CLASS by IrDeclarationOriginImpl

        val SCRIPT_CLASS by IrDeclarationOriginImpl
        val SCRIPT_THIS_RECEIVER by IrDeclarationOriginImpl
        val SCRIPT_STATEMENT by IrDeclarationOriginImpl
        val SCRIPT_EARLIER_SCRIPTS by IrDeclarationOriginImpl
        val SCRIPT_CALL_PARAMETER by IrDeclarationOriginImpl
        val SCRIPT_IMPLICIT_RECEIVER by IrDeclarationOriginImpl
        val SCRIPT_PROVIDED_PROPERTY by IrDeclarationOriginImpl
        val SCRIPT_RESULT_PROPERTY by IrDeclarationOriginImpl
        val REPL_SNIPPET_CLASS by IrDeclarationOriginImpl
        val REPL_FROM_OTHER_SNIPPET by IrDeclarationOriginImpl
        val GENERATED_DATA_CLASS_MEMBER by IrDeclarationOriginImpl
        val GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER by IrDeclarationOriginImpl
        val GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER by IrDeclarationOriginImpl
        val LOCAL_FUNCTION by IrDeclarationOriginImpl
        val LOCAL_FUNCTION_FOR_LAMBDA by IrDeclarationOriginImpl
        val CATCH_PARAMETER by IrDeclarationOriginImpl
        val UNDERSCORE_PARAMETER by IrDeclarationOriginImpl
        val DESTRUCTURED_OBJECT_PARAMETER by IrDeclarationOriginImpl
        val INSTANCE_RECEIVER by IrDeclarationOriginImpl
        val PRIMARY_CONSTRUCTOR_PARAMETER by IrDeclarationOriginImpl
        val IR_DESTRUCTURED_PARAMETER_VARIABLE by IrDeclarationOriginImpl
        val IR_TEMPORARY_VARIABLE by IrDeclarationOriginImpl
        val IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER by IrDeclarationOriginImpl
        val IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER by IrDeclarationOriginImpl
        val IR_EXTERNAL_DECLARATION_STUB by IrDeclarationOriginImpl
        val IR_EXTERNAL_JAVA_DECLARATION_STUB by IrDeclarationOriginImpl
        val IR_BUILTINS_STUB by IrDeclarationOriginImpl
        val BRIDGE by IrDeclarationOriginImpl.Synthetic
        val BRIDGE_SPECIAL by IrDeclarationOriginImpl
        val GENERATED_SETTER_GETTER by IrDeclarationOriginImpl.Synthetic

        val FIELD_FOR_ENUM_ENTRY by IrDeclarationOriginImpl
        val SYNTHETIC_HELPER_FOR_ENUM_VALUES by IrDeclarationOriginImpl.Synthetic
        val SYNTHETIC_HELPER_FOR_ENUM_ENTRIES by IrDeclarationOriginImpl.Synthetic
        val FIELD_FOR_ENUM_VALUES by IrDeclarationOriginImpl.Synthetic
        val FIELD_FOR_ENUM_ENTRIES by IrDeclarationOriginImpl.Synthetic
        val PROPERTY_FOR_ENUM_ENTRIES by IrDeclarationOriginImpl
        val FIELD_FOR_OBJECT_INSTANCE by IrDeclarationOriginImpl
        val FIELD_FOR_CLASS_CONTEXT_RECEIVER by IrDeclarationOriginImpl.Synthetic

        val ADAPTER_FOR_CALLABLE_REFERENCE by IrDeclarationOriginImpl.Synthetic
        val ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE by IrDeclarationOriginImpl
        val ADAPTER_FOR_SUSPEND_CONVERSION by IrDeclarationOriginImpl.Synthetic
        val ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION by IrDeclarationOriginImpl
        val ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR by IrDeclarationOriginImpl.Synthetic

        val GENERATED_SAM_IMPLEMENTATION by IrDeclarationOriginImpl
        val SYNTHETIC_GENERATED_SAM_IMPLEMENTATION by IrDeclarationOriginImpl.Synthetic

        val SYNTHETIC_JAVA_PROPERTY_DELEGATE by IrDeclarationOriginImpl.Synthetic

        val FIELD_FOR_OUTER_THIS by IrDeclarationOriginImpl.Synthetic
        val CONTINUATION by IrDeclarationOriginImpl.Synthetic
        val LOWERED_SUSPEND_FUNCTION by IrDeclarationOriginImpl.Synthetic

        val SHARED_VARIABLE_IN_EVALUATOR_FRAGMENT by IrDeclarationOriginImpl.Synthetic

        /**
         * Used on synthetic accessor functions generated in place of references to e.g. private symbols in non-private inline functions,
         * or `super` calls from lambdas and inline functions.
         */
        val SYNTHETIC_ACCESSOR by IrDeclarationOriginImpl.Synthetic

        /**
         * Created by `Fir2IrDeclarationStorage.fillUnboundSymbols()` to handle the code fragment
         * in the middle of code compile of `KaCompilerFacility`.
         */
        val FILLED_FOR_UNBOUND_SYMBOL by IrDeclarationOriginImpl

        val INLINE_LAMBDA by IrDeclarationOriginImpl

        /**
         * Used on synthetic `invoke` methods for `[K][Suspend]FunctionN` interfaces.
         */
        val FUNCTION_INTERFACE_MEMBER by IrDeclarationOriginImpl

        val STUB_FOR_LENIENT by IrDeclarationOriginImpl.Synthetic
    }

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

        override fun toString(): String = name

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is GeneratedByPlugin) return false
            return pluginKey == other.pluginKey
        }

        override fun hashCode(): Int = pluginKey.hashCode()
    }
}

class IrDeclarationOriginImpl(
    override val name: String,
    override val isSynthetic: Boolean = false
) : IrDeclarationOrigin, ReadOnlyProperty<Any?, IrDeclarationOriginImpl> {
    override fun toString(): String = name
    override fun getValue(thisRef: Any?, property: KProperty<*>): IrDeclarationOriginImpl = this

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IrDeclarationOriginImpl) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode()

    companion object : PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, IrDeclarationOriginImpl>> {
        override fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, IrDeclarationOriginImpl> =
            IrDeclarationOriginImpl(property.name)
    }

    object Synthetic : PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, IrDeclarationOriginImpl>> {
        override fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, IrDeclarationOriginImpl> =
            IrDeclarationOriginImpl(property.name, isSynthetic = true)
    }
}
