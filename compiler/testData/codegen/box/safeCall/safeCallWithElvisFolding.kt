fun String.foo(): String? = null

fun test(a: String?, b: String?, c: String) =
    a ?: b?.foo() ?: c
// = (a ?: b?.boo()) ?: c
// Here 'b?.foo()' returns null, which may break elvis semantics if we fold it carelessly.

fun box() = test(null, "abc", "OK")
