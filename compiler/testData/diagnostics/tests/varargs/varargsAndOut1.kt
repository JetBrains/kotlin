// !CHECK_TYPE

fun test(vararg a: String) {
    a checkType { it : _<Array<out String>> }

    foo(a) checkType { it : _<Array<out String>> }
}

fun <T> test1(vararg t: T) {
    t checkType { it : _<Array<out T>> }
}

fun <T> foo(a: Array<T>): Array<T> = a