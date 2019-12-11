// !WITH_NEW_INFERENCE
fun test() {
    run {return}
    run {}
}

fun test2() {
    run {return@test2}
    run {}
}

fun test3() {
    fun test4() {
        run {return@test3}
        run {}
    }
}

fun <T> run(f: () -> T): T { return f() }
