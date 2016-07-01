class A
interface I0<T : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>>
interface I1<T> where T : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>
interface I2<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!><!>> where T : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>

fun <E : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>> foo0() {}
fun <E> foo1() where E : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!> {}
fun <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>E : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!><!>> foo2() where E : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!> {}

val <E : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>> E.p1: Int
        get() = 1
val <E> E.p2: Int where E : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>
        get() = 1
val <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>E : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!><!>> E.p3: Int where E : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>
        get() = 1

// See KT-8200
interface X
public class EnumAttribute<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T : X<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><T><!><!>>(val klass: Class<T>) where T : Enum<T>
