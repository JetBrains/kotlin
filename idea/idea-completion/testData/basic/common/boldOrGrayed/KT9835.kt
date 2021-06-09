// FIR_COMPARISON
interface R<T>

val <T> R<T>.prop: Int get() = TODO()
fun <T> R<T>.extFun(): Int = TODO()

interface I

fun foo(r: R<out I>) {
    r.<caret>
}

// EXIST: { itemText: "prop", typeText: "Int", attributes: "bold" }
// EXIST: { itemText: "extFun", typeText: "Int", attributes: "bold" }


