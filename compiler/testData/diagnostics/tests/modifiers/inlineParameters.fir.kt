// !DIAGNOSTICS: -UNUSED_PARAMETER

<!NOTHING_TO_INLINE!>inline<!> fun foo(noinline x: Int) {}

<!NOTHING_TO_INLINE!>inline<!> fun bar(y: Int, crossinline x: String) {}

fun gav(noinline x: (Int) -> Unit, crossinline y: (String) -> Int) {}

inline fun correct(noinline x: (Int) -> Unit, crossinline y: (String) -> Int) {}

<!NOTHING_TO_INLINE!>inline<!> fun incompatible(<!INCOMPATIBLE_MODIFIERS!>noinline<!> <!INCOMPATIBLE_MODIFIERS!>crossinline<!> x: () -> String) {}

class FunctionSubtype : () -> Unit {
    override fun invoke() {}
}

<!NOTHING_TO_INLINE!>inline<!> fun functionSubtype(
    noinline f: FunctionSubtype,
    crossinline g: FunctionSubtype
) { }
