// !CHECK_TYPE
interface A<T>

interface Out<out T>

fun <T> foo(a: A<T>, o: Out<T?>): T = throw Exception("$a $o")

fun <T> doOut(o: Out<T?>): T = throw Exception("$o")

fun test(a: A<Int>, aN: A<Int?>, o: Out<Int?>) {
    val out = doOut(o)
    //T? >: Int? => T >: Int
    out checkType { _<Int>() }

    val nullable = foo(aN, o)
    //T = Int?, T? >: Int? => T = Int?
    nullable checkType { _<Int?>() }

    val notNullable = foo(a, o)
    //T = Int, T? >: Int? => T = Int
    notNullable checkType { _<Int>() }
}
