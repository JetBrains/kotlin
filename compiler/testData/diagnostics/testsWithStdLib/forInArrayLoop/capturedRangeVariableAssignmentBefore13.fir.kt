// LANGUAGE: -ProperForInArrayLoopRangeVariableAssignmentSemantic
// DIAGNOSTICS: -UNUSED_VALUE
// SKIP_TXT

fun testArrayCapturedInLocalFun() {
    var xs = arrayOf("a", "b", "c")

    fun updateXs() {
        xs = arrayOf("d", "e", "f")
    }

    for (x in xs) {
        println(x)
        updateXs()
    }
}

fun testArrayCapturedInLabmda() {
    var xs = arrayOf("a", "b", "c")

    val updateXs = { xs = arrayOf("d", "e", "f") }

    for (x in xs) {
        println(x)
        updateXs()
    }
}

fun testArrayCapturedInInlineLambda() {
    var xs = arrayOf("a", "b", "c")

    for (x in xs) {
        println(x)
        run {
            xs = arrayOf("d", "e", "f")
        }
    }
}

fun testArrayCapturedInLocalObject() {
    var xs = arrayOf("a", "b", "c")

    val updateXs = object : () -> Unit {
        override fun invoke() {
            xs = arrayOf("d", "e", "f")
        }
    }

    for (x in xs) {
        println(x)
        updateXs()
    }
}

fun testArrayCapturedInLocalClass() {
    var xs = arrayOf("a", "b", "c")

    class LocalClass {
        fun updateXs() {
            xs = arrayOf("d", "e", "f")
        }
    }

    val updater = LocalClass()

    for (x in xs) {
        println(x)
        updater.updateXs()
    }
}

fun testCapturedInLambdaAfterLoop() {
    // NB false positive
    var xs = intArrayOf(1, 2, 3)
    for (x in xs) {
        println(x)
        xs = intArrayOf(4, 5, 6)
    }
    val lambda = { xs = intArrayOf() }
    lambda()
}

fun testCapturedInLambdaInLoopAfterAssignment() {
    // NB false positive
    var xs = intArrayOf(1, 2, 3)
    for (x in xs) {
        println(x)
        xs = intArrayOf(4, 5, 6)
        val lambda = { xs = intArrayOf() }
        lambda()
    }
}

fun testCapturedInNonChangingClosure() {
    // NB false positive
    var xs = intArrayOf(1, 2, 3)
    val lambda = { println(xs) }
    for (x in xs) {
        println(x)
        xs = intArrayOf(4, 5, 6)
        lambda()
    }
}