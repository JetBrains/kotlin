// !CHECK_TYPE
interface A<T>

interface In<in T>

fun <T> foo(a: A<T>, i: In<T>): T = throw Exception("$a $i")

fun <T> doIn(i: In<T?>): T = throw Exception("$i")

fun test(a: A<Int>, aN: A<Int?>, i: In<Int?>) {
    val _in = doIn(i)
    //T? <: Int? => T <: Int?
    _in checkType { _<Int?>() }

    val notNullable = foo(a, i)
    //T = Int, T? <: Int? => T = Int
    notNullable checkType { _<Int>() }

    val nullable = foo(aN, i)
    //T = Int?, T? <: Int? => T = Int?
    nullable checkType { _<Int?>() }
}
