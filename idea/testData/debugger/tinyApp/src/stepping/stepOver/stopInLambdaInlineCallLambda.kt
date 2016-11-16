package stopInLambdaInlineCallLambda

fun main(args: Array<String>) {
    var prop = 1
    {
        inlineFun {
            {
                //Breakpoint!
                foo(12)
            }()
        }
    }()
}

inline fun inlineFun(f: () -> Unit) {
    f()
}

fun foo(a: Any) {}