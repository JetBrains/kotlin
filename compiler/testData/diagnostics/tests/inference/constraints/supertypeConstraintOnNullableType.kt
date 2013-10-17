// !DIAGNOSTICS: -BASE_WITH_NULLABLE_UPPER_BOUND
class TypeOf<T>(t: T)

trait A<T>

trait In<in T>

fun <T> foo(a: A<T>, i: In<T>): T = throw Exception("$a $i")

fun <T> doIn(i: In<T?>): T = throw Exception("$i")

fun test(a: A<Int>, aN: A<Int?>, i: In<Int?>) {
    val _in = doIn(i)
    //T? <: Int? => T <: Int?
    TypeOf(_in): TypeOf<Int?>

    val notNullable = foo(a, i)
    //T = Int, T? <: Int? => T = Int
    TypeOf(notNullable): TypeOf<Int>

    val nullable = foo(aN, i)
    //T = Int?, T? <: Int? => T = Int?
    TypeOf(nullable): TypeOf<Int?>
}
