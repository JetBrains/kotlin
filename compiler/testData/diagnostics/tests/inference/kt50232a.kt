fun test() {
    acceptMyRecursive(inferType<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>MyRecursive?<!>>())
}

fun acceptMyRecursive(value: MyRecursive?) {}

fun <R : Recursive<R>?> inferType(): R = TODO()

abstract class Recursive<R>

class MyRecursive : Recursive<MyRecursive>()
