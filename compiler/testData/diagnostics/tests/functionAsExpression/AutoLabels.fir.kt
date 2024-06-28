// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE

fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> = null!!

fun autolabel(l: List<Int>) = l.map (fun (i: Int): Int {
    return@map 4
})

fun unresolvedMapLabel(l: List<Int>) = l.map (l@ fun(i: Int): Int {
    <!RETURN_NOT_ALLOWED!>return@map<!> 4
})
