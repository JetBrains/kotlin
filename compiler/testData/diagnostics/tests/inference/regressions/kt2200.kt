//KT-2200 array(array()) breaks compiler
package n

fun main(args: Array<String>) {
    val <!UNUSED_VARIABLE!>a<!> = array(<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>array<!>())
    val <!UNUSED_VARIABLE!>a0<!> : Array<Array<Int>> = array(array())
    val a1 = array(array<Int>())
    a1 : Array<Array<Int>>
    val a2 = array<Array<Int>>(array())
    a2 : Array<Array<Int>>
}

//from library
fun <T> array(vararg t : T) : Array<T> = t as Array<T>