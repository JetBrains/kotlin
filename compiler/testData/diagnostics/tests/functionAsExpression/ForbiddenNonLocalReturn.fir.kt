// !DIAGNOSTICS: -UNUSED_VARIABLE

fun test() {
    fun bar() {
        val bas = fun() {
            return@bar
        }
    }

    val bar = fun() {
        return@test
    }
}

fun foo() {
    val bal = bag@ fun () {
        val bar = fun() {
            return@bag
        }
        return@bag
    }
}