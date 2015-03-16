// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> = null!!

fun autolabel(l: List<Int>) = l.map (fun (i: Int): Int {
    return@map 4
})

fun unresolvedMapLabel(l: List<Int>) = l.map (fun test(i: Int): Int {
    return<!UNRESOLVED_REFERENCE!>@map<!> 4
})

fun labelToFunName(l: List<Int>) = l.map (fun test(i: Int): Int {
    return@test 4
})