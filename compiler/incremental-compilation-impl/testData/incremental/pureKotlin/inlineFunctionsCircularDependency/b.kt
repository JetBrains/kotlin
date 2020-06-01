package test

inline fun b(body: () -> Unit) {
    println("I'm inline function b")
    body()
    a { println("To be inlined from b") }
}
