// !WITH_NEW_INFERENCE
fun test() {
    run {<!RETURN_NOT_ALLOWED!>return<!>}
    run {}
}

fun test2() {
    run {<!RETURN_NOT_ALLOWED!>return@test2<!>}
    run {}
}

fun test3() {
    fun test4() {
        run {<!RETURN_NOT_ALLOWED!>return@test3<!>}
        run {}
    }
}

fun <T> run(f: () -> T): T { return f() }
