// !DIAGNOSTICS: -UNUSED_PARAMETER

inline fun foo(noinline x: Int) {}

inline fun bar(y: Int, crossinline x: String) {}

fun gav(noinline x: (Int) -> Unit, crossinline y: (String) -> Int) {}

inline fun correct(noinline x: (Int) -> Unit, crossinline y: (String) -> Int) {}

inline fun incompatible(noinline crossinline x: () -> String) {}

class FunctionSubtype : () -> Unit {
    override fun invoke() {}
}

inline fun functionSubtype(
        noinline f: FunctionSubtype,
        crossinline g: FunctionSubtype
) { }
