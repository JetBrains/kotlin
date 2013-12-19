// !DIAGNOSTICS: -BASE_WITH_NULLABLE_UPPER_BOUND
// !CHECK_TYPE
trait A<T>

fun <T> foo(a: A<T>, aN: A<T?>): T = throw Exception("$a $aN")

fun <T> doA(a: A<T>): T = throw Exception("$a")

fun test(a: A<Int>, aN: A<Int?>) {
    val aa = doA(aN)
    aa checkType { it : _<Int?> }

    val nullable = foo(aN, aN)
    //T = Int?, T? = Int? => T = Int?
    nullable checkType { it : _<Int?> }

    val notNullable = foo(a, aN)
    //T = Int, T? = Int? => T = Int
    notNullable checkType { it : _<Int> }
}
