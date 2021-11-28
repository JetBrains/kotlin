//!LANGUAGE: +DefinitelyNonNullableTypes
// IGNORE_BACKEND_FIR: ANY

fun <T> asFoo(t: T) = t as (T & Any)
fun <T> safeAsFoo(t: T) = t as? (T & Any)

inline fun <reified T : CharSequence?> implicitAsFoo(x: Any) =
    if (x !is T) 0 else x.length