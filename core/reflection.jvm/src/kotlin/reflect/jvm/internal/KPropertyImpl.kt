/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorFactory
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

interface KPropertyImpl<out R> : KProperty<R>, KCallableImpl<R> {
    val javaField: Field?

    val container: KCallableContainerImpl

    override val getter: Getter<R>

    override val descriptor: PropertyDescriptor

    override val name: String get() = descriptor.name.asString()

    override val caller: FunctionCaller get() = getter.caller

    abstract class Accessor<out R> : KProperty.Accessor<R> {
        abstract override val property: KPropertyImpl<R>

        internal abstract val descriptor: PropertyAccessorDescriptor
    }

    abstract class Getter<out R> : Accessor<R>(), KProperty.Getter<R>, KCallableImpl<R> {
        override val name: String get() = "<get-${property.name}>"

        override val descriptor: PropertyGetterDescriptor by ReflectProperties.lazySoft {
            // TODO: default getter created this way won't have any source information
            property.descriptor.getter ?: DescriptorFactory.createDefaultGetter(property.descriptor)
        }

        override val caller: FunctionCaller by ReflectProperties.lazySoft {
            computeCallerForAccessor(isGetter = true)
        }
    }
}


interface KMutablePropertyImpl<R> : KMutableProperty<R>, KPropertyImpl<R> {
    override val setter: Setter<R>

    abstract class Setter<R> : KPropertyImpl.Accessor<R>(), KMutableProperty.Setter<R>, KCallableImpl<Unit> {
        abstract override val property: KMutablePropertyImpl<R>

        override val name: String get() = "<set-${property.name}>"

        override val descriptor: PropertySetterDescriptor by ReflectProperties.lazySoft {
            // TODO: default setter created this way won't have any source information
            property.descriptor.setter ?: DescriptorFactory.createDefaultSetter(property.descriptor)
        }

        override val caller: FunctionCaller by ReflectProperties.lazySoft {
            computeCallerForAccessor(isGetter = false)
        }
    }
}


private fun KPropertyImpl.Accessor<*>.computeCallerForAccessor(isGetter: Boolean): FunctionCaller {
    fun isPlatformStaticProperty() =
            property.descriptor.annotations.findAnnotation(PLATFORM_STATIC) != null

    fun computeFieldCaller(field: Field): FunctionCaller.Field = when {
        !Modifier.isStatic(field.modifiers) ->
            if (isGetter) FunctionCaller.InstanceFieldGetter(field)
            else FunctionCaller.InstanceFieldSetter(field)
        isPlatformStaticProperty() ->
            if (isGetter) FunctionCaller.PlatformStaticInObjectFieldGetter(field)
            else FunctionCaller.PlatformStaticInObjectFieldSetter(field)
        else ->
            if (isGetter) FunctionCaller.StaticFieldGetter(field)
            else FunctionCaller.StaticFieldSetter(field)
    }

    val jvmSignature = RuntimeTypeMapper.mapPropertySignature(property.descriptor)
    return when (jvmSignature) {
        is JvmPropertySignature.KotlinProperty -> {
            val accessorSignature = jvmSignature.signature.run {
                when {
                    isGetter -> if (hasGetter()) getter else null
                    else -> if (hasSetter()) setter else null
                }
            }

            val accessor = accessorSignature?.let { signature ->
                property.container.findMethodBySignature(
                        jvmSignature.proto, signature, jvmSignature.nameResolver, Visibilities.isPrivate(descriptor.visibility)
                )
            }

            when {
                accessor == null -> computeFieldCaller(property.javaField!!)
                !Modifier.isStatic(accessor.modifiers) -> FunctionCaller.InstanceMethod(accessor)
                isPlatformStaticProperty() -> FunctionCaller.PlatformStaticInObject(accessor)
                else -> FunctionCaller.StaticMethod(accessor)
            }
        }
        is JvmPropertySignature.JavaField -> {
            computeFieldCaller(jvmSignature.field)
        }
    }
}
