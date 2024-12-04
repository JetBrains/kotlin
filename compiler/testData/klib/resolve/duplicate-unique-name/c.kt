package c

fun c(indent: Int) {
    repeat(indent) { print("  ") }
    println("c")
    a.a(indent + 1)
    b.b(indent + 1)
}
