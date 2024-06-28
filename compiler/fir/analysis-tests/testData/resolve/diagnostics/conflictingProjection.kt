class None<T>
class In<in T>
class Out<out T>

fun a1(value: None<Int>) {}
fun a2(value: None<in Int>) {}
fun a3(value: None<out Int>) {}

fun a4(value: In<Int>) {}
fun a5(value: In<<!REDUNDANT_PROJECTION!>in<!> Int>) {}
fun a6(value: In<<!CONFLICTING_PROJECTION!>out<!> Int>) {}

fun a7(value: Out<Int>) {}
fun a8(value: Out<<!CONFLICTING_PROJECTION!>in<!> Int>) {}
fun a9(value: Out<<!REDUNDANT_PROJECTION!>out<!> Int>) {}

typealias A1<K> = None<K>
typealias A2<K> = None<in K>
typealias A3<K> = None<out K>

typealias A4<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> K> = None<K>
typealias A5<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> K> = None<in K>
typealias A6<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> K> = None<out K>

typealias A7<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> K> = None<K>
typealias A8<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> K> = None<in K>
typealias A9<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> K> = None<out K>

typealias A10<K> = In<K>
typealias A11<K> = In<<!REDUNDANT_PROJECTION!>in<!> K>
typealias A12<K> = In<<!CONFLICTING_PROJECTION!>out<!> K>

typealias A13<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> K> = In<K>
typealias A14<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> K> = In<<!REDUNDANT_PROJECTION!>in<!> K>
typealias A15<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> K> = In<<!CONFLICTING_PROJECTION!>out<!> K>

typealias A16<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> K> = In<K>
typealias A17<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> K> = In<<!REDUNDANT_PROJECTION!>in<!> K>
typealias A18<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> K> = In<<!CONFLICTING_PROJECTION!>out<!> K>

typealias A19<K> = Out<K>
typealias A20<K> = Out<<!CONFLICTING_PROJECTION!>in<!> K>
typealias A21<K> = Out<<!REDUNDANT_PROJECTION!>out<!> K>

typealias A22<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> K> = Out<K>
typealias A23<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> K> = Out<<!CONFLICTING_PROJECTION!>in<!> K>
typealias A24<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> K> = Out<<!REDUNDANT_PROJECTION!>out<!> K>

typealias A25<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> K> = Out<K>
typealias A26<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> K> = Out<<!CONFLICTING_PROJECTION!>in<!> K>
typealias A27<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> K> = Out<<!REDUNDANT_PROJECTION!>out<!> K>

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

fun test11(): InOuter<<!REDUNDANT_PROJECTION!>in<!> Int>.OutIntermediate<String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test12(): InOuter<<!CONFLICTING_PROJECTION!>out<!> Int>.OutIntermediate<String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()

fun test13(): InOuter<Int>.OutIntermediate<<!CONFLICTING_PROJECTION!>in<!> String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test14(): InOuter<Int>.OutIntermediate<<!REDUNDANT_PROJECTION!>out<!> String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()

fun test15(): InOuter<Int>.OutIntermediate<String>.InInner<<!REDUNDANT_PROJECTION!>in<!> Char> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test16(): InOuter<Int>.OutIntermediate<String>.InInner<<!CONFLICTING_PROJECTION!>out<!> Char> = InOuter<Int>().OutIntermediate<String>().<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>InInner<!>()

fun test17(): InOuter<<!REDUNDANT_PROJECTION!>in<!> Int>.OutIntermediate<<!REDUNDANT_PROJECTION!>out<!> String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test18(): InOuter<Int>.OutIntermediate<<!CONFLICTING_PROJECTION!>in<!> String>.InInner<<!CONFLICTING_PROJECTION!>out<!> Char> = InOuter<Int>().OutIntermediate<String>().<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>InInner<!>()

class TwoParametersOuter<T, in T1> {
    inner class TwoParametersIntermediate<out K, K1> {
        inner class InInner<in G, G1> {
        }
    }
}

fun test19(): TwoParametersOuter<Int, <!CONFLICTING_PROJECTION!>out<!> String>.TwoParametersIntermediate<<!CONFLICTING_PROJECTION!>in<!> String, Int>.InInner<Char, Char>? = null
