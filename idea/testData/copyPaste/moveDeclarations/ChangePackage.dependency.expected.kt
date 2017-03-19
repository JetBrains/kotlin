import source.*
import target.bar
import target.foo

fun f() {
    foo()
    g(bar)
    sourcePackFun()
}

fun g(p: Int){}