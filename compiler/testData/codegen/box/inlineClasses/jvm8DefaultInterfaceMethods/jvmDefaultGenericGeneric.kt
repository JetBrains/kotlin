// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.8
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

class Cell<T>(val x: T)

interface IOk {
    fun ok(): String = "OK"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineClass<T: String>(val s: T) : IOk

fun test(cell: Cell<InlineClass<String>>): String = cell.x.ok()

fun box() = test(Cell(InlineClass("FAIL")))
