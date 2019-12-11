class A
interface I0<T : A<Int>>
interface I1<T> where T : A<Int>
interface I2<T : A<Int>> where T : A<Int>

fun <E : A<Int>> foo0() {}
fun <E> foo1() where E : A<Int> {}
fun <E : A<Int>> foo2() where E : A<Int> {}

val <E : A<Int>> E.p1: Int
        get() = 1
val <E> E.p2: Int where E : A<Int>
        get() = 1
val <E : A<Int>> E.p3: Int where E : A<Int>
        get() = 1

// See KT-8200
interface X
public class EnumAttribute<T : X<T>>(val klass: Class<T>) where T : Enum<T>
