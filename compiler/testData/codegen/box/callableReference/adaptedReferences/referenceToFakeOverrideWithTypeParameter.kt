// WITH_STDLIB
// ISSUE: KT-67307

interface Base<T, ID> {
    fun <S : T> foo(x: List<S>): List<S>
}

interface Derived : Base<CharSequence, String>

class Impl : Derived {
    override fun <S : CharSequence> foo(x: List<S>): List<S> {
        result = x.first() as String
        return x
    }
}

var result = "Fail"

fun test(d: Derived) {
    fun consume(f: (List<CharSequence>) -> Unit) {
        f(listOf("OK"))
    }
    consume(d::foo)
}

fun box(): String {
    test(Impl())
    return result
}
