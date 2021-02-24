class A
interface I0<T : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!>>
interface I1<T> where T : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!>
interface I2<T : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!>> where T : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!>

fun <E : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!>> foo0() {}
fun <E> foo1() where E : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!> {}
fun <E : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!>> foo2() where E : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!> {}

val <E : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!>> E.p1: Int
        get() = 1
val <E> E.p2: Int where E : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!>
        get() = 1
val <E : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!>> E.p3: Int where E : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int><!>
        get() = 1

// See KT-8200
interface X
public class EnumAttribute<T : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>X<T><!>>(val klass: Class<T>) where T : Enum<T>
