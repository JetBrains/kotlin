// !WITH_NEW_INFERENCE
// !CHECK_TYPE

//KT-2200 array(array()) breaks compiler
package n

fun main() {
    val <!UNUSED_VARIABLE!>a<!> = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>array<!>(<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>array<!>())
    val <!UNUSED_VARIABLE!>a0<!> : Array<Array<Int>> = array(array())
    val a1 = array(array<Int>())
    checkSubtype<Array<Array<Int>>>(a1)
    val a2 = array<Array<Int>>(array())
    checkSubtype<Array<Array<Int>>>(a2)
}

//from library
@Suppress("UNCHECKED_CAST")
fun <T> array(vararg t : T) : Array<T> = t as Array<T>