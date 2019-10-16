package stopInNonInlinedLambdaInInlineCallWithClosure

fun main(args: Array<String>) {
    var prop = 1
    inlineFun {
        notInlineFun {
            //Breakpoint!
            foo(prop)
            foo(prop)
        }
    }
}

inline fun inlineFun(f: () -> Unit) {
    f()
}

fun notInlineFun(f: () -> Unit) {
    f()
}

fun foo(a: Any) {}