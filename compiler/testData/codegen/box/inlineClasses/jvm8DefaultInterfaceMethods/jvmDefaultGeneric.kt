// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.8

class Cell<T>(val x: T)

interface IOk {
    fun ok(): String = "OK"
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlineClass(val s: String) : IOk

fun test(cell: Cell<InlineClass>): String = cell.x.ok()

fun box() = test(Cell(InlineClass("FAIL")))
