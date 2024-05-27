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
        private object _IrDeclarationOriginImpl {
            operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, IrDeclarationOriginImpl> =
                IrDeclarationOriginImpl(property.name)

            object _Synthetic {
                operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, IrDeclarationOriginImpl> =
                    IrDeclarationOriginImpl(property.name, isSynthetic = true)
            }
        }

        val DEFINED by _IrDeclarationOriginImpl
        val FAKE_OVERRIDE by _IrDeclarationOriginImpl
        val FOR_LOOP_ITERATOR by _IrDeclarationOriginImpl
        val FOR_LOOP_VARIABLE by _IrDeclarationOriginImpl
        val FOR_LOOP_IMPLICIT_VARIABLE by _IrDeclarationOriginImpl
        val PROPERTY_BACKING_FIELD by _IrDeclarationOriginImpl
        val DEFAULT_PROPERTY_ACCESSOR by _IrDeclarationOriginImpl
        val DELEGATE by _IrDeclarationOriginImpl._Synthetic
        val PROPERTY_DELEGATE by _IrDeclarationOriginImpl
        val DELEGATED_PROPERTY_ACCESSOR by _IrDeclarationOriginImpl
        val DELEGATED_MEMBER by _IrDeclarationOriginImpl
        val ENUM_CLASS_SPECIAL_MEMBER by _IrDeclarationOriginImpl
        val FUNCTION_FOR_DEFAULT_PARAMETER by _IrDeclarationOriginImpl._Synthetic
        val MASK_FOR_DEFAULT_FUNCTION by _IrDeclarationOriginImpl._Synthetic
        val DEFAULT_CONSTRUCTOR_MARKER by _IrDeclarationOriginImpl._Synthetic
        val METHOD_HANDLER_IN_DEFAULT_FUNCTION by _IrDeclarationOriginImpl._Synthetic
        val MOVED_DISPATCH_RECEIVER by _IrDeclarationOriginImpl
        val MOVED_EXTENSION_RECEIVER by _IrDeclarationOriginImpl
        val MOVED_CONTEXT_RECEIVER by _IrDeclarationOriginImpl

        val FILE_CLASS by _IrDeclarationOriginImpl
        val SYNTHETIC_FILE_CLASS by _IrDeclarationOriginImpl._Synthetic
        val JVM_MULTIFILE_CLASS by _IrDeclarationOriginImpl
        val ERROR_CLASS by _IrDeclarationOriginImpl

        val SCRIPT_CLASS by _IrDeclarationOriginImpl
        val SCRIPT_THIS_RECEIVER by _IrDeclarationOriginImpl
        val SCRIPT_STATEMENT by _IrDeclarationOriginImpl
        val SCRIPT_EARLIER_SCRIPTS by _IrDeclarationOriginImpl
        val SCRIPT_CALL_PARAMETER by _IrDeclarationOriginImpl
        val SCRIPT_IMPLICIT_RECEIVER by _IrDeclarationOriginImpl
        val SCRIPT_PROVIDED_PROPERTY by _IrDeclarationOriginImpl
        val SCRIPT_RESULT_PROPERTY by _IrDeclarationOriginImpl
        val REPL_SNIPPET_CLASS by _IrDeclarationOriginImpl
        val REPL_FROM_OTHER_SNIPPET by _IrDeclarationOriginImpl
        val GENERATED_DATA_CLASS_MEMBER by _IrDeclarationOriginImpl
        val GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER by _IrDeclarationOriginImpl
        val GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER by _IrDeclarationOriginImpl
        val LOCAL_FUNCTION by _IrDeclarationOriginImpl
        val LOCAL_FUNCTION_FOR_LAMBDA by _IrDeclarationOriginImpl
        val CATCH_PARAMETER by _IrDeclarationOriginImpl
        val UNDERSCORE_PARAMETER by _IrDeclarationOriginImpl
        val DESTRUCTURED_OBJECT_PARAMETER by _IrDeclarationOriginImpl
        val INSTANCE_RECEIVER by _IrDeclarationOriginImpl
        val PRIMARY_CONSTRUCTOR_PARAMETER by _IrDeclarationOriginImpl
        val IR_DESTRUCTURED_PARAMETER_VARIABLE by _IrDeclarationOriginImpl
        val IR_TEMPORARY_VARIABLE by _IrDeclarationOriginImpl
        val IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER by _IrDeclarationOriginImpl
        val IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER by _IrDeclarationOriginImpl
        val IR_EXTERNAL_DECLARATION_STUB by _IrDeclarationOriginImpl
        val IR_EXTERNAL_JAVA_DECLARATION_STUB by _IrDeclarationOriginImpl
        val IR_BUILTINS_STUB by _IrDeclarationOriginImpl
        val BRIDGE by _IrDeclarationOriginImpl._Synthetic
        val BRIDGE_SPECIAL by _IrDeclarationOriginImpl
        val GENERATED_SETTER_GETTER by _IrDeclarationOriginImpl._Synthetic

        val FIELD_FOR_ENUM_ENTRY by _IrDeclarationOriginImpl
        val SYNTHETIC_HELPER_FOR_ENUM_VALUES by _IrDeclarationOriginImpl._Synthetic
        val SYNTHETIC_HELPER_FOR_ENUM_ENTRIES by _IrDeclarationOriginImpl._Synthetic
        val FIELD_FOR_ENUM_VALUES by _IrDeclarationOriginImpl._Synthetic
        val FIELD_FOR_ENUM_ENTRIES by _IrDeclarationOriginImpl._Synthetic
        val PROPERTY_FOR_ENUM_ENTRIES by _IrDeclarationOriginImpl
        val FIELD_FOR_OBJECT_INSTANCE by _IrDeclarationOriginImpl
        val FIELD_FOR_CLASS_CONTEXT_RECEIVER by _IrDeclarationOriginImpl._Synthetic

        val ADAPTER_FOR_CALLABLE_REFERENCE by _IrDeclarationOriginImpl._Synthetic
        val ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE by _IrDeclarationOriginImpl
        val ADAPTER_FOR_SUSPEND_CONVERSION by _IrDeclarationOriginImpl._Synthetic
        val ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION by _IrDeclarationOriginImpl
        val ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR by _IrDeclarationOriginImpl._Synthetic

        val GENERATED_SAM_IMPLEMENTATION by _IrDeclarationOriginImpl
        val SYNTHETIC_GENERATED_SAM_IMPLEMENTATION by _IrDeclarationOriginImpl._Synthetic

        val SYNTHETIC_JAVA_PROPERTY_DELEGATE by _IrDeclarationOriginImpl._Synthetic

        val FIELD_FOR_OUTER_THIS by _IrDeclarationOriginImpl._Synthetic
        val CONTINUATION by _IrDeclarationOriginImpl._Synthetic
        val LOWERED_SUSPEND_FUNCTION by _IrDeclarationOriginImpl._Synthetic

        val SHARED_VARIABLE_IN_EVALUATOR_FRAGMENT by _IrDeclarationOriginImpl._Synthetic

        /**
         * Used on synthetic accessor functions generated in place of references to e.g. private symbols in non-private inline functions,
         * or `super` calls from lambdas and inline functions.
         */
        val SYNTHETIC_ACCESSOR by _IrDeclarationOriginImpl._Synthetic

        /**
         * Created by `Fir2IrDeclarationStorage.fillUnboundSymbols()` to handle the code fragment
         * in the middle of code compile of `KaCompilerFacility`.
         */
        val FILLED_FOR_UNBOUND_SYMBOL by _IrDeclarationOriginImpl

        val INLINE_LAMBDA by _IrDeclarationOriginImpl
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
