// FIR_IDENTICAL
// FIR_DUMP
package one.two

import one.two.MyDataClass.MyClass as Alias

fun main() {
    val value = Alias()
    val data = MyDataClass(value)
    val copy = data.copy(value)
    val prop: Alias = data.prop
    val component1: Alias = data.component1()
    val (destructuring: Alias) = (data)
}

class MyClass

data class MyDataClass(val prop: MyClass) {
    class MyClass
}
