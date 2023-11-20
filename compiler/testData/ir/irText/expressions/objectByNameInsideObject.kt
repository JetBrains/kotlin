// FIR_IDENTICAL

open class Base(val f1: () -> Any)

object Thing : Base({ Thing }) {
    fun test1() = Thing
    fun test2() = this
}
