// !DIAGNOSTICS: -BASE_WITH_NULLABLE_UPPER_BOUND
class TypeOf<T>(t: T)

trait A<T>

fun <T> foo(a: A<T>, aN: A<T?>): T = throw Exception("$a $aN")

fun <T> doA(a: A<T>): T = throw Exception("$a")

fun test(a: A<Int>, aN: A<Int?>) {
    val aa = doA(aN)
    TypeOf(aa): TypeOf<Int?>

    val nullable = foo(aN, aN)
    //T = Int?, T? = Int? => T = Int?
    TypeOf(nullable): TypeOf<Int?>

    val notNullable = foo(a, aN)
    //T = Int, T? = Int? => T = Int
    TypeOf(notNullable): TypeOf<Int>
}
