// WITH_STDLIB
// IGNORE_BACKEND: JVM_IR

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