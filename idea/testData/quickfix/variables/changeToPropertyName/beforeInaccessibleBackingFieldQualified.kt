// "Change '$a' to 'a'" "true"
package foo
val a = 5
class A {
    val b = foo.<caret>$a
}