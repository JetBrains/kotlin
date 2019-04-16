// WITH_RUNTIME
fun test() {
    emptyList<Pair<Int, Int>>().<caret>mapIndexed { index, _ ->
        emptyList<String>().mapIndexed { number, s ->
            if (s.isEmpty()) return@mapIndexed number
            else 42
        }
        return@mapIndexed index
    }
}