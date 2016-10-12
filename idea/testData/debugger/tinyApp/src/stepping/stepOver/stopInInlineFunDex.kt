package stopInInlineFunDex

fun main(args: Array<String>) {
    inlineFun()
}

inline fun inlineFun() {
    var i = 1
    //Breakpoint!
    i++
    i++
}