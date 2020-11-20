// FIR_COMPARISON
fun run(action: () -> Unit) = action()

fun test() {
    fun aa() {}
    val aaa = 10

    run {
        fun aabb() {}
        val aaabb = 20

        <caret>

        Unit // remove this
    }
}

// EXIST: aa
// EXIST: aaa
// EXIST: aabb
// EXIST: aaabb
