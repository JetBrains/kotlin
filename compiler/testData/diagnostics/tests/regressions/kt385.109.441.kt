// KT-385 type inference does not work properly`
// KT-109 Good code is red: type arguments are not inferred
// KT-441 Exception in type inference when multiple overloads accepting an integer literal are accessible

import java.util.*

infix fun <T> Iterator<T>.foreach(operation: (element: T) -> Unit)  : Unit { while(hasNext()) operation(next()) }

infix fun <T> Iterator<T>.foreach(operation: (index: Int, element: T) -> Unit) : Unit {
    var k = 0
    while(hasNext())
        operation(k++, next())
}

fun <T> Iterable<T>.foreach(operation: (element: T) -> Unit) : Unit = iterator() foreach operation

fun <T> Iterable<T>.foreach(operation: (index: Int, element: T) -> Unit) : Unit = iterator() foreach operation

fun box() : String {
    return generic_invoker( { "OK"} )
}

fun <T> generic_invoker(gen :  () -> T) : T {
    return gen()
}

fun println(message : Int) { System.out.println(message) }
fun println(message : Long) { System.out.println(message) }

fun main() {

    println(run { 1 })
}
