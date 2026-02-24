/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.desc
import java.lang.reflect.Method
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.FunctionBase
import kotlin.metadata.Modality
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createDefaultType
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller.CallMode.CALL_BY_NAME
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller.CallMode.POSITIONAL_CALL
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller.Origin.JAVA
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.jvmErasure

internal class JavaAnnotationConstructor(
    private val klass: KClassImpl<*>,
) : ReflectKCallableImpl<Any?>(KCallableOverriddenStorage.EMPTY), ReflectKFunction, FunctionBase<Any?>, FunctionWithAllInvokes {
    // Java annotations do not impose any order of methods inside them, so we consider them lexicographic here for stability
    private val methods = klass.java.declaredMethods.sortedBy { it.name }

    override val container: KDeclarationContainerImpl get() = klass
    override val rawBoundReceiver: Any? get() = null
    override val signature: String by lazy(PUBLICATION) {
        parameters.joinToString(separator = "", prefix = "<init>(", postfix = ")V") { it.type.jvmErasure.java.desc }
    }

    override val name: String get() = "<init>"

    override val visibility: KVisibility? get() = klass.java.modifiers.computeVisibilityForJavaModifiers()
    override val modality: Modality get() = Modality.FINAL
    override val isSuspend: Boolean get() = false
    override val isInline: Boolean get() = false
    override val isExternal: Boolean get() = false
    override val isOperator: Boolean get() = false
    override val isInfix: Boolean get() = false

    override val isPrimaryConstructor: Boolean get() = true
    override val isPackagePrivate: Boolean get() = klass.java.modifiers.isPackagePrivate

    override val returnType: KType by lazy(PUBLICATION) {
        klass.createDefaultType()
    }

    override val allParameters: List<KParameter> get() = parameters

    override val parameters: List<KParameter> by lazy(PUBLICATION) {
        methods.withIndex().map { (index, method) -> JavaAnnotationConstructorParameter(this, method, index) }
    }

    override val typeParameters: List<KTypeParameter> get() = emptyList()
    override val annotations: List<Annotation> get() = emptyList()
    override val arity: Int get() = methods.size
    override val overridden: Collection<ReflectKFunction> get() = emptyList()

    override val caller: Caller<*> by lazy(PUBLICATION) {
        AnnotationConstructorCaller(klass.java, methods.map { it.name }, POSITIONAL_CALL, JAVA, methods)
    }

    override val callerWithDefaults: Caller<*> by lazy(PUBLICATION) {
        AnnotationConstructorCaller(klass.java, methods.map { it.name }, CALL_BY_NAME, JAVA, methods)
    }

    override fun replaceContainerForFakeOverride(
        container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage,
    ): ReflectKCallable<Any?> =
        error("Annotation constructors cannot be copied: $this")

    override fun equals(other: Any?): Boolean {
        val that = other.asReflectFunction() ?: return false
        return container == that.container && name == that.name && signature == that.signature && rawBoundReceiver == that.rawBoundReceiver
    }

    override fun hashCode(): Int =
        (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
        ReflectionObjectRenderer.renderFunction(this)
}

internal class JavaAnnotationConstructorParameter(
    override val callable: JavaAnnotationConstructor,
    private val method: Method,
    override val index: Int,
) : ReflectKParameter() {
    override val name: String get() = method.name
    override val type: KType by lazy(PUBLICATION) {
        method.genericReturnType.toKType(emptyMap(), TypeNullability.NOT_NULL, isForAnnotationParameter = true)
    }
    override val kind: KParameter.Kind get() = KParameter.Kind.VALUE
    override val isVararg: Boolean get() = name == "value" && method.returnType.isArray
    override val annotations: List<Annotation> get() = emptyList()

    override val declaresDefaultValue: Boolean get() = isOptional

    override val isOptional: Boolean get() = method.defaultValue != null
}
