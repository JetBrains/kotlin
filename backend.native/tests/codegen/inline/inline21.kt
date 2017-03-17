inline fun foo2(block2: () -> Int) : Int {
    println("foo2")
    return block2()
}

inline fun foo1(block1: () -> Int) : Int {
    println("foo1")
    return foo2(block1)
}

fun bar(block: () -> Int) : Int {
    println("bar")
    return foo1(block)
}

fun main(args: Array<String>) {
    println(bar { 33 })
}
