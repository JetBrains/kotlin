// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidPrivateToThisUnboundCallableReferences
// ISSUE: KT-82640

class A<in T>(private val x: T) {
    // Invisible member/reference should be here (otherwise we have CCE in runtime)
    fun <S> leak() = A<S>::<!INVISIBLE_MEMBER("x; private/*private to this*/; 'A'")!>x<!>

    fun <S> leak(a: A<S>) = a::<!INVISIBLE_MEMBER!>x<!>

    fun <S> nonLeak() = A<S>::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>y<!>

    fun <S> nonLeak(a: A<S>) = a::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>y<!>

    private val <S> A<S>.y: S
        get() = null <!UNCHECKED_CAST!>as S<!>
}

open class Base()
class Child : Base()

fun main() {
    val y = A(Base())
    val ref = y.leak<Child>()
    val v2 = ref(y)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, in, localProperty, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
