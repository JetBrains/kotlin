fun main(args : Array<String>) {
    val x = foo {
        it + 1
    }
    println(x)
}

fun foo(f: (Int) -> Int) = f(42)