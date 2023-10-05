// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE)
annotation class Ann

expect fun valueParameterType(arg: @Ann String)

expect fun returnType(): @Ann String

expect fun <T : @Ann Any> methodTypeParamBound()

expect class OnClassTypeParamBound<T : @Ann Any>

expect fun <T> typeParamBoundInWhere() where T : @Ann Any

interface I1
interface I2

expect fun <T> severalBounds() where T : I1, T : @Ann I2

expect fun <!NO_ACTUAL_FOR_EXPECT{JVM}!><T><!> severalBoundsDifferentOrder() where T : I2, T : @Ann I1

expect fun <!NO_ACTUAL_FOR_EXPECT{JVM}!><T><!> lessTypeParamBoundsOnActual() where T : I1, T : @Ann I2

expect fun @Ann Any.onReceiver()

expect class OnClassSuper : @Ann I1

expect class OnClassSuperDifferentOrder : I1, @Ann I2

expect class OnClassSuperMoreOnActual : @Ann I2

interface I3<T>

expect class OnClassSuperTypeParams<T> : I3<@Ann T>

expect fun deepInParamsTypes(arg: I3<I3<@Ann Any>>)

interface I4<T, U>

expect fun starProjection(arg: I4<*, @Ann Any>)

expect fun <T> typeArgWithVariance(t: I3<out @Ann T>)

class WithNested<T> {
    inner class Nested<U>
}

expect fun qualifierPartsMatching(arg: WithNested<String>.Nested<@Ann String>)

expect fun qualifierPartsNonMatching(arg: WithNested<String>.Nested<@Ann String>)

expect fun funTypeVsUserType<!NO_ACTUAL_FOR_EXPECT{JVM}!>(arg: () -> @Ann String)<!>

expect fun funcTypeReturnType(arg: () -> @Ann Any)

expect fun funcTypeReceiverType(arg: @Ann Any.() -> Unit)

expect fun funcTypeArgType(arg: (arg: @Ann Any) -> Unit)

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun valueParameterType(arg: String) {}

actual fun returnType(): String = ""

actual fun <T : Any> methodTypeParamBound() {}

actual class OnClassTypeParamBound<T : Any>

actual fun <T> typeParamBoundInWhere() where T : Any {}

actual fun <T> severalBounds() where T : I1, T : I2 {}

actual fun <!ACTUAL_WITHOUT_EXPECT!><T><!> severalBoundsDifferentOrder() where T : @Ann I1, T : I2 {}

actual fun <!ACTUAL_WITHOUT_EXPECT!><T><!> lessTypeParamBoundsOnActual() where T : @Ann I2 {}

actual fun Any.onReceiver() {}

actual class OnClassSuper : I1

actual class OnClassSuperDifferentOrder : @Ann I2, I1

actual class OnClassSuperMoreOnActual : I1, I2

actual class OnClassSuperTypeParams<T> : I3<T>

actual fun deepInParamsTypes(arg: I3<I3<Any>>) {}

actual fun starProjection(arg: I4<*, Any>) {}

actual fun <T> typeArgWithVariance(t: I3<out T>) {}

actual fun qualifierPartsMatching(arg: WithNested<String>.Nested<@Ann String>) {}

actual fun qualifierPartsNonMatching(arg: WithNested<@Ann String>.Nested<String>) {}

actual fun funTypeVsUserType<!ACTUAL_WITHOUT_EXPECT!>(arg: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>kotlin.jvm.functions.Function0<String><!>)<!> {}

actual fun funcTypeReturnType(arg: () -> Any) {}

actual fun funcTypeReceiverType(arg: Any.() -> Unit) {}

actual fun funcTypeArgType(arg: (arg: Any) -> Unit) {}
