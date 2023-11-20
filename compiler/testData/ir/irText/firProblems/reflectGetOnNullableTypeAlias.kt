// FIR_IDENTICAL
// SKIP_KLIB_TEST
// WITH_REFLECT

import kotlin.reflect.KProperty1

private typealias PropAlias<T> = KProperty1<T, Any?>?

fun box(): String {
    val backRefProp: PropAlias<Foo> = Foo::bar
    if (backRefProp != null) {
        return backRefProp.get(Foo()) as String
    }
    return "FAIL"
}

class Foo {
    val bar: String = "OK"
}
