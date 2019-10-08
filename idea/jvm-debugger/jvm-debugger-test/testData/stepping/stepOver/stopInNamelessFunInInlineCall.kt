package stopInNamelessFunInInlineCall

fun main(args: Array<String>) {
    val a = 1

    inlineFun(fun (): Unit {
        {
            //Breakpoint!
            foo(a)
            foo(a)
        }()
    })
}

inline fun inlineFun(f: () -> Any) {
    f()
}

fun foo(a: Any) {}