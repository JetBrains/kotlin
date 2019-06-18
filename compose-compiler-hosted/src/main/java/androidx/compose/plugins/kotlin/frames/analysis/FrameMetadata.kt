package androidx.compose.plugins.kotlin.frames.analysis

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.hasBackingField
import org.jetbrains.kotlin.types.KotlinType

/**
 * Helpers for creating the new properties, fields and properties of the framed class and the framed
 * record
 *
 * - Framed properties are all public properties of the class.
 * - A framed record has
 *   - a corresponding property for each of the framed class' public properties
 *   - a `create` method that creates a new instance of the record
 *   - an `assign` method that copies values from the value into the record
 * - All framed properties redirect to the current readable or writable record for the class
 *   corresponding to the current open frame.
 *
 *   For example, given the declaration:
 *
 *   @Model
 *   class MyModel {
 *     var some: String = "Default some"
 *     var data: String = "Default data"
 *
 *   }
 *
 *   The class is transformed into something like:
 *
 *   class MyModel: Framed {
 *     var some: String
 *       get() = (_readable(next) as MyModel_Record).some
 *       set(value) { (_writable(next) as MyModel_Record).some = value }
 *     var data: String
 *       get() = ((_readable(next) as MyModel_Record).data
 *       set(value) { (_writable(next, this) as MyModel_Record).data = value }
 *
 *     private var _firstFrameRecord: MyModelRecord? = null
 *
 *     override var firstFrameRecord: Record get() = _firstFrameRecord
 *     override fun prependFrameRecord(value: Record) {
 *       value.next = _firstFrameRecord
 *       _firstFrameRecord = value
 *     }
 *
 *     init {
 *       next = MyModel_Record()
 *       (next as MyModel_Record).some = "Default some"
 *       (next as MyModel_Record).data = "Default data"
 *     }
 *   }
 *
 *   class MyModel_Record : AbstractRecord {
 *     @JvmField var some: String
 *     @JvmField var data: String
 *
 *     override fun create(): Record = MyModel_Record()
 *     override fun assign(value: Record) {
 *       some = (value as MyModel_Record).some
 *       data = (value as MyModel_Record).data
 *     }
 *   }
 */
class FrameMetadata(private val framedClassDescriptor: ClassDescriptor) {
    private val builtIns = DefaultBuiltIns.Instance

    /**
     * Get the list of properties on the framed class that should be framed
     */
    fun getFramedProperties(bindingContext: BindingContext) =
        framedClassDescriptor.unsubstitutedMemberScope.getContributedDescriptors().mapNotNull {
            if (it is PropertyDescriptor &&
                it.kind == CallableMemberDescriptor.Kind.DECLARATION &&
                it.isVar && it.hasBackingField(bindingContext)
            ) it else null
        }

    /**
     * Get the list of the record's properties (on for each public property of the framed object)
     */
    fun getRecordPropertyDescriptors(
        recordClassDescriptor: ClassDescriptor,
        bindingContext: BindingContext
    ): List<PropertyDescriptor> {
        return getFramedProperties(bindingContext).map {
            syntheticProperty(
                recordClassDescriptor,
                it.name,
                it.returnType!!
            )
        }
    }

    /**
     * Get the list of the record's methods (create and assign)
     */
    fun getRecordMethodDescriptors(
        container: ClassDescriptor,
        recordDescriptor: ClassDescriptor
    ): List<SimpleFunctionDescriptor> {
        return listOf(
            // fun create(): <record>
            syntheticMethod(
                container,
                "create",
                recordDescriptor.defaultType
            ),
            // fun assign(value: <record>)
            syntheticMethod(
                container, "assign", container.builtIns.unitType,
                Parameter(
                    "value",
                    recordDescriptor.defaultType
                )
            )
        )
    }

    fun firstFrameDescriptor(recordTypeDescriptor: ClassDescriptor): SimpleFunctionDescriptor =
        syntheticMethod(
            framedClassDescriptor,
            "getFirstFrameRecord",
            recordTypeDescriptor.defaultType
        )

    fun prependFrameRecordDescriptor(
        recordTypeDescriptor: ClassDescriptor
    ): SimpleFunctionDescriptor =
        syntheticMethod(
            framedClassDescriptor, "prependFrameRecord", builtIns.unitType,
            Parameter(
                "value",
                recordTypeDescriptor.defaultType
            )
        )
}

private fun syntheticProperty(
    container: ClassDescriptor,
    name: Name,
    type: KotlinType,
    visibility: Visibility = Visibilities.PUBLIC,
    readonly: Boolean = false
): PropertyDescriptor =
    PropertyDescriptorImpl.create(
        container, Annotations.EMPTY, Modality.OPEN, Visibilities.PUBLIC, true,
        name, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
        false, false, true, true, false,
        false
    ).apply {
        val getter = PropertyGetterDescriptorImpl(
            this,
            Annotations.EMPTY,
            Modality.OPEN,
            visibility,
            false,
            false,
            false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            null,
            SourceElement.NO_SOURCE
        ).apply { initialize(type) }
        val setter = if (readonly) null else PropertySetterDescriptorImpl(
            this,
            Annotations.EMPTY,
            Modality.OPEN,
            visibility,
            false,
            false,
            false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            null,
            SourceElement.NO_SOURCE
        ).apply {
            initialize(
                PropertySetterDescriptorImpl.createSetterParameter(this, type, Annotations.EMPTY)
            )
        }
        initialize(getter, setter)
        setType(
            type,
            emptyList<TypeParameterDescriptor>(),
            container.thisAsReceiverParameter,
            null as ReceiverParameterDescriptor?
        )
    }

private data class Parameter(val name: Name, val type: KotlinType) {
    constructor(name: String, type: KotlinType) : this(Name.identifier(name), type)
}

private fun syntheticMethod(
    container: ClassDescriptor,
    name: Name,
    returnType: KotlinType,
    vararg parameters: Parameter
): SimpleFunctionDescriptor =
    SimpleFunctionDescriptorImpl.create(
        container,
        Annotations.EMPTY,
        name,
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        SourceElement.NO_SOURCE
    ).apply {
        val parameterDescriptors = parameters.map {
            ValueParameterDescriptorImpl(
                this,
                null,
                0,
                Annotations.EMPTY,
                it.name,
                it.type,
                false,
                false,
                false,
                null,
                SourceElement.NO_SOURCE
            )
        }

        initialize(
            null,
            container.thisAsReceiverParameter,
            emptyList<TypeParameterDescriptor>(),
            parameterDescriptors,
            returnType,
            Modality.FINAL,
            Visibilities.PUBLIC
        )
    }

private fun syntheticMethod(
    container: ClassDescriptor,
    name: String,
    returnType: KotlinType,
    vararg parameters: Parameter
): SimpleFunctionDescriptor =
    syntheticMethod(
        container,
        Name.identifier(name),
        returnType,
        *parameters
    )
