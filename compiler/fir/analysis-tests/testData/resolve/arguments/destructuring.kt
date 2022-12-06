data class C(val x: Int, val y: String)

fun foo1(block: (C) -> Unit) = block(C(0, ""))
fun foo2(block: (C, C) -> Unit) = block(C(0, ""), C(0, ""))

fun test() {
    foo1 { (x, y) -> C(x, y) }
    foo1 { (x: Int, y: String) -> C(x, y) }
    foo1 { (x: String, y: Int) -> C(<!ARGUMENT_TYPE_MISMATCH!>x<!>, <!ARGUMENT_TYPE_MISMATCH!>y<!>) }
    foo2 { (x, y), (z, w) -> C(x + z, y + w) }
    foo2 { (x: Int, y: String), (z: Int, w: String) -> C(x + z, y + w) }
    foo2 { (x: String, y: Int), (z: String, w: Int) -> C(<!ARGUMENT_TYPE_MISMATCH!>x + z<!>, <!ARGUMENT_TYPE_MISMATCH!>y + w<!>) }
}
