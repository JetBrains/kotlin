package foo

fun box(): String {
    var foo = { x: String -> x + "K" }
    return foo.invoke("O")
}