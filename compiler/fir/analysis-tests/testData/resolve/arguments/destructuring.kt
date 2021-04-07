data class C(val x: Int, val y: String)

fun foo1(block: (C) -> Unit) = block(C(0, ""))
fun foo2(block: (C, C) -> Unit) = block(C(0, ""), C(0, ""))

fun test() {
    foo1 { (x, y) -> C(x, y) }
    foo1 { (x: Int, y: String) -> C(x, y) }
    foo1 { (x: String, y: Int) -> <!INAPPLICABLE_CANDIDATE!>C<!>(x, y) }
    foo2 { (x, y), (z, w) -> C(x + z, y + w) }
    foo2 { (x: Int, y: String), (z: Int, w: String) -> C(x + z, y + w) }
    foo2 { (x: String, y: Int), (z: String, w: Int) -> <!INAPPLICABLE_CANDIDATE!>C<!>(x + z, y + w) }
}
