package stopInInlineCallLocalFunLambda

fun main(args: Array<String>) {
    var prop = 1
    inlineFun {
        fun local() {
            {
                //Breakpoint!
                foo(12)
            }()
        }

        local()
    }
}

inline fun inlineFun(f: () -> Unit) {
    f()
}

fun foo(a: Any) {}