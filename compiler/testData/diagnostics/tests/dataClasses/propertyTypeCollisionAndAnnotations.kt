// FIR_IDENTICAL
// FIR_DUMP
package one.two

import one.two.MyDataClass.MyClass as Alias

@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

fun main() {
    val value = Alias<Alias<Int>>()
    val data = MyDataClass(value)
    val copy = data.copy(value)
    val prop: Alias<Alias<Int>> = data.prop
    val component1: Alias<Alias<Int>> = data.component1()
    val (destructuring: Alias<Alias<Int>>) = (data)
}

const val constant = ""

class MyClass<A>

data class MyDataClass(val prop: @Anno(0 + constant) MyClass<@Anno(1 + constant) MyClass<@Anno(2 + constant) Int>>) {
    class MyClass<B>
    companion object {
        const val constant = 0
    }
}
