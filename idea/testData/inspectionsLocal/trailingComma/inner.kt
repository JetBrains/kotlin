// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas
// FIX: Add line break

fun a(
    a: Int, b: Any = fun(a: Int,) {},<caret>) {

}