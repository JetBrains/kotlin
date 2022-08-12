fun test() {
    acceptMyRecursive(<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION!>inferType<!><<!UPPER_BOUND_VIOLATED!>MyRecursive?<!>>())
}

fun acceptMyRecursive(value: MyRecursive?) {}

fun <R : Recursive<R>?> inferType(): R = TODO()

abstract class Recursive<R>

class MyRecursive : Recursive<MyRecursive>()
