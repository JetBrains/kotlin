// !DIAGNOSTICS: -BASE_WITH_NULLABLE_UPPER_BOUND
// !CHECK_TYPE
trait A<T>

trait In<in T>

fun <T> foo(a: A<T>, i: In<T>): T = throw Exception("$a $i")

fun <T> doIn(i: In<T?>): T = throw Exception("$i")

fun test(a: A<Int>, aN: A<Int?>, i: In<Int?>) {
    val _in = doIn(i)
    //T? <: Int? => T <: Int?
    _in checkType { it : _<Int?> }

    val notNullable = foo(a, i)
    //T = Int, T? <: Int? => T = Int
    notNullable checkType { it : _<Int> }

    val nullable = foo(aN, i)
    //T = Int?, T? <: Int? => T = Int?
    nullable checkType { it : _<Int?> }
}
