// WITH_STDLIB

fun use(c: suspend (Pair<Int, Int>) -> Unit) {}

fun blackhole(a: Any) {}

fun test() {
    use { (a, b) ->
    }
    use { (a, b) ->
        blackhole(a)
    }
    use { (a, b) ->
        blackhole(b)
    }
    use {
        blackhole(it)
    }
}
