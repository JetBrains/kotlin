class None<T>
class In<in T>
class Out<out T>

fun a1(value: None<Int>) {}
fun a2(value: None<in Int>) {}
fun a3(value: None<out Int>) {}

fun a7(value: Out<Int>) {}
fun a8(value: Out<in Int>) {}
fun a9(value: Out<out Int>) {}

typealias A1<K> = None<K>
typealias A2<K> = None<in K>
typealias A3<K> = None<out K>

typealias A13<in K> = In<K>
typealias A14<in K> = In<in K>
typealias A15<in K> = In<out K>
