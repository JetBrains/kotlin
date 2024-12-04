
open class A
class B : A()

fun compareClasses(a: Any, b: Any) = a::class == b::class

fun isA(a: Any) = a::class == A::class

inline fun <reified T> isT(a: Any) = a::class == T::class

fun box(): String {
    if (!compareClasses("a", "b")) return "Fail 1"
    if (compareClasses(Any(), "")) return "Fail 2"
    if (!isA(A())) return "Fail 3"
    if (isA(B())) return "Fail 4"
    if (!isT<A>(A())) return "Fail 5"
    if (isT<A>(B())) return "Fail 6"
    if (isT<B>(A())) return "Fail 7"
    if (isT<Any>(B())) return "Fail 8"
    if (!isT<Int>(10)) return "Fail 9"
    if (!isT<Int>(10 as Any)) return "Fail 10"
    return "OK"
}
