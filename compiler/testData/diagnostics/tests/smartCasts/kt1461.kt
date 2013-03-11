//KT-1461 Front-end complains on overload resolution when superclass property is accessed from string template and implicit type cast is performed
package f

open class Super(val property : String) {}

class Sub(str : String) : Super(str) {}

fun foo(sup : Super, sub : Sub) {
    if (sup is Sub) {
        println("${sup.property}")
        println(sup.property)
    }
    println("${sub.property}")
    println(sub.property)
}

//from library
fun println(<!UNUSED_PARAMETER!>message<!> : Any?) { throw Exception() }