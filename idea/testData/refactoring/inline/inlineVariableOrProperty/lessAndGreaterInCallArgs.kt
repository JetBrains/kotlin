fun f(a : (Any, Any) -> Unit, b : Int, c : Int, d : Int, e : Int, f : Int) {
    val <caret>g = b < c
    a(g, d > (e + f))
}