class None<T>
class In<in T>
class Out<out T>

fun a1(value: None<Int>) {}
fun a2(value: None<in Int>) {}
fun a3(value: None<out Int>) {}

fun a4(value: In<Int>) {}
fun a5(value: In<in Int>) {}
fun a6(value: In<out Int>) {}

fun a7(value: Out<Int>) {}
fun a8(value: Out<in Int>) {}
fun a9(value: Out<out Int>) {}

typealias A1<K> = None<K>
typealias A2<K> = None<in K>
typealias A3<K> = None<out K>

typealias A4<in K> = None<K>
typealias A5<in K> = None<in K>
typealias A6<in K> = None<out K>

typealias A7<out K> = None<K>
typealias A8<out K> = None<in K>
typealias A9<out K> = None<out K>

typealias A10<K> = In<K>
typealias A11<K> = In<in K>
typealias A12<K> = In<out K>

typealias A13<in K> = In<K>
typealias A14<in K> = In<in K>
typealias A15<in K> = In<out K>

typealias A16<out K> = In<K>
typealias A17<out K> = In<in K>
typealias A18<out K> = In<out K>

typealias A19<K> = Out<K>
typealias A20<K> = Out<in K>
typealias A21<K> = Out<out K>

typealias A22<in K> = Out<K>
typealias A23<in K> = Out<in K>
typealias A24<in K> = Out<out K>

typealias A25<out K> = Out<K>
typealias A26<out K> = Out<in K>
typealias A27<out K> = Out<out K>

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
fun test12(): InOuter<out Int>.OutIntermediate<String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()

fun test13(): InOuter<Int>.OutIntermediate<in String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test14(): InOuter<Int>.OutIntermediate<out String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()

fun test15(): InOuter<Int>.OutIntermediate<String>.InInner<in Char> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test16(): InOuter<Int>.OutIntermediate<String>.InInner<out Char> = InOuter<Int>().OutIntermediate<String>().InInner()

fun test17(): InOuter<in Int>.OutIntermediate<out String>.InInner<Char> = InOuter<Int>().OutIntermediate<String>().InInner()
fun test18(): InOuter<Int>.OutIntermediate<in String>.InInner<out Char> = InOuter<Int>().OutIntermediate<String>().InInner()
