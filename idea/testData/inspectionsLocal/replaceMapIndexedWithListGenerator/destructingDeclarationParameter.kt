// PROBLEM: none
// WITH_RUNTIME
fun test() {
    emptyList<Pair<Int, Int>>().<caret>mapIndexed { index, (l, r) ->
        l + r + index
    }.forEach(::println)
}