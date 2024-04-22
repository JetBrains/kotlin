// LANGUAGE: +ValueClasses
// ISSUE: KT-66976
// WITH_STDLIB
// MODULE: lib

interface Lib {
    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>fun <!VIRTUAL_MEMBER_HIDDEN!>hashCode<!>(): Boolean = true<!>
    fun box(): Boolean
}

interface Lib1 {
    fun box(): Boolean = true
}

interface Lib2 {
    fun box(): Boolean
}

interface Lib3 {
    fun box(): Boolean = true
}

// MODULE: main(lib)
interface I1 {
    fun <T> equals(other: A1): Boolean = true
    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>fun <!VIRTUAL_MEMBER_HIDDEN!>hashCode<!>(): Boolean = true<!>
    fun box(): Boolean = true
}

@JvmInline
value <!RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS("I1; box"), RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS("I1; equals")!>class A1<!>(val i: Int) : I1

fun main1() {
    val a1 = A1(1)
    val a2 = A1(2)
    a1.equals<Int>(a2)
}

interface I2 {
    fun <T> equals(other: A2<T>): Boolean = true
    fun box(): Boolean
}

interface I2_ : I2

@JvmInline
value <!ABSTRACT_MEMBER_NOT_IMPLEMENTED, RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS("I2; equals")!>class A2<!><T>(val i: Int) : I2_

fun main2() {
    val a1 = A2<Int>(1)
    val a2 = A2<String>(2)
    a1.equals(a2)
}


interface I3 {
    fun <T> equals(other: A3): Boolean = true
    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>fun <!VIRTUAL_MEMBER_HIDDEN!>hashCode<!>(): Boolean = true<!>
    fun box(): Boolean = true
}

@JvmInline
value <!RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS("I3; box"), RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS("I3; equals")!>class A3<!>(val i: Int, val i1: Int) : I3

fun main3() {
    val a1 = A3(1, -1)
    val a2 = A3(2, -2)
    a1.equals<Int>(a2)
}

interface I4 {
    fun <T> equals(other: A4<T>): Boolean = true
}

@JvmInline
value <!ABSTRACT_MEMBER_NOT_IMPLEMENTED, RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS("I4; equals")!>class A4<!><T>(val i: Int, val i1: Int) : I4, Lib

fun main4() {
    val a1 = A4<Int>(1, -1)
    val a2 = A4<String>(2, -2)
    a1.equals(a2)
}


interface I5 {
    fun <T> equals(other: A5<T>): Boolean = true
}

@JvmInline
value <!CANNOT_INFER_VISIBILITY, MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED, RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS("Lib1; box"), RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS("I5; equals")!>class A5<!><T>(val i: Int, val i1: Int) : I5, Lib1, Lib2, Lib3

fun main5() {
    val a1 = A5<Int>(1, -1)
    val a2 = A5<String>(2, -2)
    a1.equals(a2)
}

abstract class AC {
    fun equals(arg: AC): Boolean = true
}

@JvmInline
value class A6(val i: Int) : <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>AC<!>()
