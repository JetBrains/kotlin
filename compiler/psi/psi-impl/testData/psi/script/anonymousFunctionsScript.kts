// COMPILATION_ERRORS

val anonymous = fun(a: Int) = a

run(fun() {
    fun localInAnonymous() {}
})

fun () {}
