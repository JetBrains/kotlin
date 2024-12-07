// JVM_ABI_K1_K2_DIFF: KT-70625 K2 generates more correct signatures of callable references

interface I

interface Foo<in L : I, in M>

interface Bar<T> {
    fun test(x: Foo<*, T>)
}

fun foo(x: Any) {
    if (x is Bar<*>) {
        x::test
    }
}

fun box(): String = "OK"
