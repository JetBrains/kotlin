// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// JVM_TARGET: 1.8

class Cell<T>(val x: T)

interface IOk {
    fun ok(): String = "OK"
}

inline class InlineClass(val s: String) : IOk

fun test(cell: Cell<InlineClass>): String = cell.x.ok()

fun box() = test(Cell(InlineClass("FAIL")))
