// !CHECK_TYPE
interface A<T>

fun <T> foo(a: A<T>, aN: A<T?>): T = throw Exception("$a $aN")

fun <T> doA(a: A<T>): T = throw Exception("$a")

fun test(a: A<Int>, aN: A<Int?>) {
    val aa = doA(aN)
    aa checkType {  _<Int?>() }

    val nullable = foo(aN, aN)
    //T = Int?, T? = Int? => T = Int?
    nullable checkType { _<Int?>() }

    val notNullable = foo(a, aN)
    //T = Int, T? = Int? => T = Int
    notNullable checkType { _<Int>() }
}
