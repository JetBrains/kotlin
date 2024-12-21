// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-74107 (blocked by)
// LANGUAGE: +NestedTypeAliases

class Bar {
    inner class Inner {
        val p: String
            get() = "OK"
    }
    inner class Inner2<T>(val p: T)

    class Nested<T>(val p: T)

    typealias TAtoInner = Inner
    typealias TA2toInner2<K> = Inner2<K>
    typealias TAtoNested = Nested<String>

    fun bar(): String {
        if (TAtoInner().p != "OK") return "FAIL"
        if (TA2toInner2("OK").p != "OK") return "FAIL"
        if (TAtoNested("OK").p != "OK") return "FAIL"

        return "OK"
    }
}

fun box(): String = Bar().bar()