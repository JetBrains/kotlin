// "Create type alias 'X'" "false"
// ACTION: Create class 'X'
// ACTION: Create interface 'X'
// ERROR: Unresolved reference: X
package p

open class A<T : List<T>>

fun foo(a: A<<caret>X<Int>>) {

}
