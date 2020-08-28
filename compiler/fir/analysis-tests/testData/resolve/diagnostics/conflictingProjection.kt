class None<T>
class In<in T>
class Out<out T>

fun a1(value: None<Int>) {}
fun a2(value: None<in Int>) {}
fun a3(value: None<out Int>) {}

fun a4(value: In<Int>) {}
fun a5(value: In<in Int>) {}
fun a6(value: <!CONFLICTING_PROJECTION!>In<out Int><!>) {}

fun a7(value: Out<Int>) {}
fun a8(value: <!CONFLICTING_PROJECTION!>Out<in Int><!>) {}
fun a9(value: Out<out Int>) {}

typealias A1<K> = None<K>
typealias A2<K> = None<in K>
typealias A3<K> = None<out K>

typealias A4<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in K<!>> = None<K>
typealias A5<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in K<!>> = None<in K>
typealias A6<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in K<!>> = None<out K>

typealias A7<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out K<!>> = None<K>
typealias A8<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out K<!>> = None<in K>
typealias A9<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out K<!>> = None<out K>

typealias A10<K> = In<K>
typealias A11<K> = In<in K>
typealias A12<K> = <!CONFLICTING_PROJECTION!>In<out K><!>

typealias A13<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in K<!>> = In<K>
typealias A14<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in K<!>> = In<in K>
typealias A15<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in K<!>> = <!CONFLICTING_PROJECTION!>In<out K><!>

typealias A16<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out K<!>> = In<K>
typealias A17<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out K<!>> = In<in K>
typealias A18<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out K<!>> = <!CONFLICTING_PROJECTION!>In<out K><!>

typealias A19<K> = Out<K>
typealias A20<K> = <!CONFLICTING_PROJECTION!>Out<in K><!>
typealias A21<K> = Out<out K>

typealias A22<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in K<!>> = Out<K>
typealias A23<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in K<!>> = <!CONFLICTING_PROJECTION!>Out<in K><!>
typealias A24<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in K<!>> = Out<out K>

typealias A25<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out K<!>> = Out<K>
typealias A26<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out K<!>> = <!CONFLICTING_PROJECTION!>Out<in K><!>
typealias A27<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out K<!>> = Out<out K>

class Outer<T> {
    inner class Intermediate<K> {
        inner class Inner<G> {

        }
    }
}

fun test1(): Outer<Int>.Intermediate<String>.Inner<Char> = Outer<Int>().Intermediate<String>().Inner()

fun test2(): Outer<in Int>.Intermediate<String>.Inner<Char> = Outer<Int>().Intermediate<String>().Inner()
fun test3(): Outer<out Int>.Intermediate<String>.Inner<Char> = Outer<Int>().Intermediate<String>().Inner()

fun test4(): Outer<Int>.Intermediate<in String>.Inner<Char> = Outer<Int>().Intermediate<String>().Inner()
fun test5(): Outer<Int>.Intermediate<out String>.Inner<Char> = Outer<Int>().Intermediate<String>().Inner()

fun test6(): Outer<Int>.Intermediate<String>.Inner<in Char> = Outer<Int>().Intermediate<String>().Inner()
fun test7(): Outer<Int>.Intermediate<String>.Inner<out Char> = Outer<Int>().Intermediate<String>().Inner()

fun test8(): Outer<in Int>.Intermediate<out String>.Inner<Char> = Outer<Int>().Intermediate<String>().Inner()
fun test9(): Outer<Int>.Intermediate<in String>.Inner<out Char> = Outer<Int>().Intermediate<String>().Inner()

class InOuter<in T> {
    inner class OutIntermediate<out K> {
        inner class InInner<in G> {

        }
    }
}

fun test10(): InOuter<Int>.OutIntermediate<String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()

fun test11(): InOuter<in Int>.OutIntermediate<String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test12(): <!CONFLICTING_PROJECTION!>InOuter<out Int>.OutIntermediate<String>.InInner<Char><!> = InOuter<Int>().OutIntermediate<String>().InInner()

fun test13(): <!CONFLICTING_PROJECTION!>InOuter<Int>.OutIntermediate<in String>.InInner<Char><!> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test14(): InOuter<Int>.OutIntermediate<out String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()

fun test15(): InOuter<Int>.OutIntermediate<String>.InInner<in Char> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test16(): <!CONFLICTING_PROJECTION!>InOuter<Int>.OutIntermediate<String>.InInner<out Char><!> = InOuter<Int>().OutIntermediate<String>().InInner()

fun test17(): InOuter<in Int>.OutIntermediate<out String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test18(): <!CONFLICTING_PROJECTION!>InOuter<Int>.OutIntermediate<in String>.InInner<out Char><!> = InOuter<Int>().OutIntermediate<String>().InInner()
