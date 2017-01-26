data class A(val x: String, val y: String)
fun foo(a: A, block: (Int, A, String) -> String): String = block(1, a, "#")
fun bb() = foo(A("O", "K")) { i: Int, (x, y)<caret>, v: String -> i.toString() + x + y + v }