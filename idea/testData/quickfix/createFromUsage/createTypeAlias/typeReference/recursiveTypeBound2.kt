// "Create type alias 'X'" "false"
// ACTION: Create class 'X'
// ACTION: Create interface 'X'
// ERROR: Unresolved reference: X
package p

open class A<T, U : Map<T, U>>

fun foo(a: A<List<String>, <caret>X<Int>>) {

}