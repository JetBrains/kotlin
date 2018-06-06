fun test1(x: String?) =
    if (x == null) 0 else x.length

fun <T : CharSequence?> test2(x: T) =
    if (x == null) 0 else x.length

inline fun <reified T : CharSequence?> test3(x: Any) =
    if (x !is T) 0 else x.length

inline fun <reified T : CharSequence> test4(x: Any?) =
    if (x !is T) 0 else x.length

fun <T : S?, S> test5(x: T, fn: (S) -> Unit) {
    if (x != null) fn(x)
}