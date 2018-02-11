// !WITH_NEW_INFERENCE
package foo

import kotlin.reflect.KProperty

class A1 {
    var a1: String by MyProperty1()
    var b1: String by getMyProperty1()
}

var c1: String by getMyProperty1()
var d1: String by MyProperty1()

fun <A, B> getMyProperty1() = MyProperty1<A, B>()

class MyProperty1<R, T> {

    operator fun getValue(thisRef: R, desc: KProperty<*>): T {
        println("get $thisRef ${desc.name}")
        throw Exception()
    }

    operator fun setValue(thisRef: R, desc: KProperty<*>, value: T) {
        println("set $thisRef ${desc.name} $value")
    }
}

//--------------------------

class A2 {
    var a2: String by MyProperty2()
    var b2: String by getMyProperty2()
}

var c2: String by getMyProperty2()
var d2: String by MyProperty2()

fun <A> getMyProperty2() = MyProperty2<A>()

class MyProperty2<T> {

    operator fun getValue(thisRef: Any?, desc: KProperty<*>): T {
        println("get $thisRef ${desc.name}")
        throw Exception()
    }

    operator fun setValue(thisRef: Any?, desc: KProperty<*>, value: T) {
        println("set $thisRef ${desc.name} $value")
    }
}

//--------------------------

class A3 {
    var a3: String by <!NI;DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>MyProperty3()<!>
    var b3: String by <!NI;DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>getMyProperty3()<!>
}

var c3: String by <!NI;DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>getMyProperty3()<!>
var d3: String by <!NI;DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>MyProperty3()<!>

fun <A> getMyProperty3() = MyProperty3<A>()

class MyProperty3<T> {

    operator fun getValue(thisRef: T, desc: KProperty<*>): String {
        println("get $thisRef ${desc.name}")
        return ""
    }

    operator fun setValue(thisRef: Any?, desc: KProperty<*>, value: T) {
        println("set $thisRef ${desc.name} $value")
    }
}

//--------------------------
fun println(a: Any?) = a
