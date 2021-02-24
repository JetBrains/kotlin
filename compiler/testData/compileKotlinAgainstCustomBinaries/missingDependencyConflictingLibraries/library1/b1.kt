package b

import a.A
import a.AA
import a.AAA

interface B1 {
    fun produceA(): A<String>.Inner<Int, Unit>
    fun produceAA(): AA<Int>.Inner<Unit, String>
    fun <T> produceAGeneric(t: T): A<String>.Inner<Int, Unit>
    fun produceAAA(): AAA<String>.Inner<Int>.InnerInner<Unit>
}
