// "Create extension function 'A.times'" "true"
class A

operator fun A.times(i: Int) = this

fun test() {
    A() * <caret>"1"
}