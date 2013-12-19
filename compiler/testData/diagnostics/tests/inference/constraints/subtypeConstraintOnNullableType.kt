// !DIAGNOSTICS: -BASE_WITH_NULLABLE_UPPER_BOUND
// !CHECK_TYPE
trait A<T>

trait Out<out T>

fun <T> foo(a: A<T>, o: Out<T?>): T = throw Exception("$a $o")

fun <T> doOut(o: Out<T?>): T = throw Exception("$o")

fun test(a: A<Int>, aN: A<Int?>, o: Out<Int?>) {
    val out = doOut(o)
    //T? >: Int? => T >: Int
    out checkType { it : _<Int> }

    val nullable = foo(aN, o)
    //T = Int?, T? >: Int? => T = Int?
    nullable checkType { it : _<Int?> }

    val notNullable = foo(a, o)
    //T = Int, T? >: Int? => T = Int
    notNullable checkType { it : _<Int> }
}
