// "Create type alias 'X'" "true"
// ERROR: Unresolved reference: Dummy
package p

open class A<T, U : List<T>>

fun foo(a: A<List<String>, <caret>X<Int>>) {

}