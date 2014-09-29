// "Create function 'compareTo' from usage" "true"

class A<T>(val n: T)

fun test() {
    A(1) <caret>< 2
}