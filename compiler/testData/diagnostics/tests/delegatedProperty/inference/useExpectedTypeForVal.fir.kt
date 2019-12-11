package foo

import kotlin.reflect.KProperty

class A1 {
    val a1: String by MyProperty1()
    val b1: String by getMyProperty1()
}

val c1: String by getMyProperty1()
val d1: String by MyProperty1()

fun <A, B> getMyProperty1() = MyProperty1<A, B>()

class MyProperty1<R, T> {

    operator fun getValue(thisRef: R, desc: KProperty<*>): T {
        println("get $thisRef ${desc.name}")
        throw Exception()
    }
}

//--------------------------

class A2 {
    val a2: String by MyProperty2()
    val b2: String by getMyProperty2()
}

val c2: String by getMyProperty2()
val d2: String by MyProperty2()

fun <A> getMyProperty2() = MyProperty2<A>()

class MyProperty2<T> {

    operator fun getValue(thisRef: Any?, desc: KProperty<*>): T {
        println("get $thisRef ${desc.name}")
        throw Exception()
    }
}

//--------------------------

class A3 {
    val a3: String by MyProperty3()
    val b3: String by getMyProperty3()
}

val c3: String by getMyProperty3()
val d3: String by MyProperty3()

fun <A> getMyProperty3() = MyProperty3<A>()

class MyProperty3<T> {

    operator fun getValue(thisRef: T, desc: KProperty<*>): String {
        println("get $thisRef ${desc.name}")
        return ""
    }
}

//--------------------------
fun println(a: Any?) = a
