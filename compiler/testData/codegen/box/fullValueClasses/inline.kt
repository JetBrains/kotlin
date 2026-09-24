// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value class A(val a1: Int, val a2: Any)

inline fun withDefaultValue(value: A = A(1, A(2, 3))) = value.a1 + (value.a2 as A).a1

inline fun callOtherInlineFun() = withDefaultValue()

inline fun callableReference(): (Int, Any) -> A = ::A

inline fun <reified T : A> withReified(): T = A(1, A(2, 3)).a2 as T

inline val inlineProperty: A get() = A(1, A(2, 3))

fun box(): String {
    if (withDefaultValue() != 3) return "Fail1"
    if (callOtherInlineFun() != 3) return "Fail2"
    if (callableReference()(1, 2).a1 != 1) return "Fail3"
    if (withReified<A>().a1 != 2) return "Fail4"
    if (inlineProperty.a1 != 1) return "Fail5"

    return "OK"
}
