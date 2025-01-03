// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE)
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

<!CONFLICTING_OVERLOADS!>expect fun valueParameterType(arg: @Ann String)<!>

<!CONFLICTING_OVERLOADS!>expect fun returnType(): @Ann String<!>

<!CONFLICTING_OVERLOADS!>expect fun <T : @Ann Any> methodTypeParamBound()<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>OnClassTypeParamBound<!><T : @Ann Any>

<!CONFLICTING_OVERLOADS!>expect fun <T> typeParamBoundInWhere()<!> where T : @Ann Any

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I1<!>
interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I2<!>

<!CONFLICTING_OVERLOADS!>expect fun <T> severalBounds()<!> where T : I1, T : @Ann I2

<!CONFLICTING_OVERLOADS!>expect fun <!NO_ACTUAL_FOR_EXPECT{JVM}!><T><!> severalBoundsDifferentOrder()<!> where T : I2, T : @Ann I1

<!CONFLICTING_OVERLOADS!>expect fun <!NO_ACTUAL_FOR_EXPECT{JVM}!><T><!> lessTypeParamBoundsOnActual()<!> where T : I1, T : @Ann I2

<!CONFLICTING_OVERLOADS!>expect fun @Ann Any.onReceiver()<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>OnClassSuper<!> : @Ann I1

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>OnClassSuperDifferentOrder<!> : I1, @Ann I2

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>OnClassSuperMoreOnActual<!> : @Ann I2

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I3<!><T>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>OnClassSuperTypeParams<!><T> : I3<@Ann T>

<!CONFLICTING_OVERLOADS!>expect fun deepInParamsTypes(arg: I3<I3<@Ann Any>>)<!>

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I4<!><T, U>

<!CONFLICTING_OVERLOADS!>expect fun starProjection(arg: I4<*, @Ann Any>)<!>

<!CONFLICTING_OVERLOADS!>expect fun <T> typeArgWithVariance(t: I3<out @Ann T>)<!>

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>WithNested<!><T> {
    inner class Nested<U>
}

<!CONFLICTING_OVERLOADS!>expect fun qualifierPartsMatching(arg: WithNested<String>.Nested<@Ann String>)<!>

<!CONFLICTING_OVERLOADS!>expect fun qualifierPartsNonMatching(arg: WithNested<String>.Nested<@Ann String>)<!>

<!CONFLICTING_OVERLOADS!>expect fun funTypeVsUserType<!NO_ACTUAL_FOR_EXPECT{JVM}!>(arg: () -> @Ann String)<!><!>

<!CONFLICTING_OVERLOADS!>expect fun funcTypeReturnType(arg: () -> @Ann Any)<!>

<!CONFLICTING_OVERLOADS!>expect fun funcTypeReceiverType(arg: @Ann Any.() -> Unit)<!>

<!CONFLICTING_OVERLOADS!>expect fun funcTypeArgType(arg: (arg: @Ann Any) -> Unit)<!>

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
