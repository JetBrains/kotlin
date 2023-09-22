package baz

import kotlin.reflect.KProperty

class A(outer: Outer) {
    var i: String by  + getMyConcreteProperty()
    var d: String by  getMyConcreteProperty() - 1
    var c: String by  O.getMyProperty()
    var g: String by  outer.getContainer().getMyProperty()


    var b: String by  <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>foo(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getMyProperty<!>())<!>
    var r: String by  <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>foo(outer.getContainer().<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getMyProperty<!>())<!>
    var e: String by  + <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getMyProperty<!>())
    var f: String by  <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getMyProperty<!>()) - 1
}

fun <A, B> foo(a: Any?) = MyProperty<A, B>()

fun <A, B> getMyProperty() = MyProperty<A, B>()

fun getMyConcreteProperty() = MyProperty<Any?, String>()

class MyProperty<R, T> {

    operator fun getValue(thisRef: R, desc: KProperty<*>): T {
        println("get $thisRef ${desc.name}")
        return null <!UNCHECKED_CAST!>as T<!>
    }

    operator fun setValue(thisRef: R, desc: KProperty<*>, value: T) {
        println("set $thisRef ${desc.name} $value")
    }
}

operator fun <R, T> MyProperty<R, T>.unaryPlus() = MyProperty<R, T>()
operator fun <R, T> MyProperty<R, T>.minus(i: Int) = MyProperty<R, T>()

object O {
    fun <A, B> getMyProperty() = MyProperty<A, B>()
}

interface MyPropertyContainer {
    fun <R, T> getMyProperty(): MyProperty<R, T>
}

interface Outer {
    fun getContainer(): MyPropertyContainer
}

// -----------------
fun println(a: Any?) = a
