// !DIAGNOSTICS: -BASE_WITH_NULLABLE_UPPER_BOUND
class TypeOf<T>(t: T)

trait A<T>

trait Out<out T>

fun <T> foo(a: A<T>, o: Out<T?>): T = throw Exception("$a $o")

fun <T> doOut(o: Out<T?>): T = throw Exception("$o")

fun test(a: A<Int>, aN: A<Int?>, o: Out<Int?>) {
    val out = doOut(o)
    //T? >: Int? => T >: Int
    TypeOf(out): TypeOf<Int>

    val nullable = foo(aN, o)
    //T = Int?, T? >: Int? => T = Int?
    TypeOf(nullable): TypeOf<Int?>

    val notNullable = foo(a, o)
    //T = Int, T? >: Int? => T = Int
    TypeOf(notNullable): TypeOf<Int>
}
