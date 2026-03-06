val a = "OK"

annotation class Anno {
    class Inner {
        val shouldNotBeEvaluated = a
    }
}

fun box(): String {
    return Anno.Inner().shouldNotBeEvaluated
}
