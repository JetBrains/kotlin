// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T: Any> bar(a: Array<T>): Array<T?> =  null!!

fun test1(a: Array<Int>) {
    val r: Array<Int?> = bar(a)
    val t = bar(a)
    t checkType { it : _<Array<Int?>> }
}

fun <T: Any> foo(l: Array<T>): Array<Array<T?>> = null!!

fun test2(a: Array<out Int>) {
    val r: Array<out Array<out Int?>> = foo(a)
    val t = foo(a)
    t checkType { it : _<Array<out Array<out Int?>>> }
}

fun test3(a: Array<in Int>) {
    val r: Array<out Array<in Int?>> = foo(a)
    val t = foo(a)
    t checkType { it : _<Array<out Array<in Int?>>> }
}
