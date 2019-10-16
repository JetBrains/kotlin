package stepOverDeclarationInInlineFun

fun main(args: Array<String>) {
    foo { 1 }
}

inline fun foo(f: () -> Int): Int {
    //Breakpoint!
    val a = 15
    return f()
}