// IGNORE_BACKEND_K2: ANY
// DUMP_IR
// ISSUE: KT-73454

class Foo<T> {
    inner class Inner(val p: String)
    inner class Inner2<T2>
}

typealias InnerAlias<K> = Foo<K>.Inner
typealias InnerAlias2<K, K2> = Foo<K>.Inner2<K2>
typealias InnerAlias3<K, K2> = Foo<K2>.Inner2<K>

fun box(): String {
    val foo = Foo<String>()
    if (foo.InnerAlias("OK").p != "OK") return "FAIL"
    foo.InnerAlias2<String, Int>()
    foo.InnerAlias3<Int, String>()

    val aliasedInner = Foo<String>::InnerAlias
    if (aliasedInner(foo, "OK").p != "OK") return "FAIL"

    return "OK"
}