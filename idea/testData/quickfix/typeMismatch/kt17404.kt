// "Change type from 'Int' to 'X'" "true"
// ERROR: Cannot use 'X' as reified type parameter. Use a class instead.
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
inline fun <reified T> inlineReified0(f: (T) -> T) = {}
fun <X> callInlineReified3() = inlineReified0<X>({ x: <caret>Int -> x })