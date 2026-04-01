/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Type
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.enums.EnumEntries
import kotlin.metadata.Modality
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.internal.calls.Caller

internal class JavaEnumEntriesKProperty(
    private val enumClass: KClassImpl<out Enum<*>>,
) : ReflectKCallableImpl<EnumEntries<*>>(KCallableOverriddenStorage.EMPTY), ReflectKProperty<EnumEntries<*>>, KProperty0<EnumEntries<*>> {
    private val result = enumEntriesMethod(null, enumClass.java.enumConstants) as EnumEntries<*>

    override val container: KDeclarationContainerImpl get() = enumClass
    override val rawBoundReceiver: Any? get() = null
    override val signature: String get() = ENUM_ENTRIES_SIGNATURE

    override val name: String get() = "entries"

    override val visibility: KVisibility get() = KVisibility.PUBLIC
    override val modality: Modality get() = Modality.FINAL
    override val isSuspend: Boolean get() = false
    override val isConst: Boolean get() = false
    override val isLateinit: Boolean get() = false
    override val javaField: Field? get() = null
    override val isPackagePrivate: Boolean get() = false

    override val returnType: KType by lazy(PUBLICATION) {
        EnumEntries::class.createType(listOf(KTypeProjection.invariant(enumClass.createType())))
    }

    override val allParameters: List<KParameter> get() = emptyList()
    override val parameters: List<KParameter> get() = emptyList()
    override val typeParameters: List<KTypeParameter> get() = emptyList()
    override val annotations: List<Annotation> get() = emptyList()

    override fun getDelegate(): Any? = null

    override fun shallowCopy(
        container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage,
    ): ReflectKCallable<EnumEntries<*>> = JavaEnumEntriesKProperty(enumClass)

    override val caller: Caller<*> = JavaEnumEntriesCaller()
    override val callerWithDefaults: Caller<*>? get() = null

    override fun invoke(): EnumEntries<*> = get()
    override fun get(): EnumEntries<*> = result

    override val getter: KProperty0.Getter<EnumEntries<*>> by lazy(PUBLICATION) { Getter() }

    private inner class JavaEnumEntriesCaller : Caller<Nothing?> {
        @OptIn(ExperimentalStdlibApi::class)
        override val returnType: Type get() = this@JavaEnumEntriesKProperty.returnType.javaType
        override val member: Nothing? get() = null
        override val parameterTypes: List<Type> get() = emptyList()
        override fun call(args: Array<*>): Any = result.also { checkArguments(args) }
    }

    private inner class Getter : ReflectKCallableImpl<EnumEntries<*>>(KCallableOverriddenStorage.EMPTY), KProperty0.Getter<EnumEntries<*>> {
        override val property: ReflectKProperty<EnumEntries<*>> get() = this@JavaEnumEntriesKProperty
        override val container: KDeclarationContainerImpl get() = property.container
        override val rawBoundReceiver: Any? get() = null

        override val name: String get() = "<get-${property.name}>"

        override val modality: Modality get() = property.modality
        override val visibility: KVisibility? get() = property.visibility
        override val isInline: Boolean get() = false
        override val isExternal: Boolean get() = false
        override val isOperator: Boolean get() = false
        override val isInfix: Boolean get() = false
        override val isSuspend: Boolean get() = false
        override val isPackagePrivate: Boolean get() = false

        override val allParameters: List<KParameter> get() = property.allParameters
        override val parameters: List<KParameter> get() = property.parameters
        override val typeParameters: List<KTypeParameter> get() = property.typeParameters
        override val annotations: List<Annotation> get() = property.annotations

        override val returnType: KType get() = property.returnType

        override val caller: Caller<*> get() = property.caller
        override val callerWithDefaults: Caller<*>? get() = null

        override fun invoke(): EnumEntries<*> = this@JavaEnumEntriesKProperty.get()

        override fun shallowCopy(
            container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage,
        ): ReflectKCallable<EnumEntries<*>> =
            error("Property accessors can only be copied by copying the corresponding property")

        override fun equals(other: Any?): Boolean = other is Getter && property == other.property
        override fun hashCode(): Int = property.hashCode()
        override fun toString(): String = "getter of $property"
    }

    override fun equals(other: Any?): Boolean {
        val that = other.asReflectProperty() ?: return false
        return container == that.container && name == that.name && signature == that.signature && rawBoundReceiver == that.rawBoundReceiver
    }

    override fun hashCode(): Int =
        (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
        ReflectionObjectRenderer.renderProperty(this)

    companion object {
        const val ENUM_ENTRIES_SIGNATURE = "getEntries()Lkotlin/enums/EnumEntries;"

        // Here we call the internal @PublishedApi `enumEntries` function from stdlib via reflection, to avoid duplicating the logic of
        // creating and serializing/deserializing `EnumEntries`. This function is used from generated code, so it won't be changed/removed
        // due to stdlib's binary compatibility guarantees.
        private val enumEntriesMethod: Method = stdlibClassLoader.loadClass("kotlin.enums.EnumEntriesKt").declaredMethods.single { method ->
            // Can't call `getDeclaredMethod("enumEntries", Array<Enum<*>>::class.java)` because of KT-13924, so we use `declaredMethods`.
            method.name == "enumEntries" && method.parameterTypes.singleOrNull()?.let { parameter ->
                parameter.isArray && parameter.componentType == Enum::class.java
            } == true
        }
    }
}
