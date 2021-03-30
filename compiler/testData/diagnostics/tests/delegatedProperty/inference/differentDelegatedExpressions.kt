// !WITH_NEW_INFERENCE
package baz

import kotlin.reflect.KProperty

class A(outer: Outer) {
    var i: String by  + getMyConcreteProperty()
    var d: String by  getMyConcreteProperty() - 1
    var c: String by  O.getMyProperty()
    var g: String by  outer.getContainer().getMyProperty()


    var b: String by  <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE{NI}, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE{NI}!>foo(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>getMyProperty<!>())<!>
    var r: String by  <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE{NI}, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE{NI}!>foo(outer.getContainer().<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>getMyProperty<!>())<!>
    var e: String by  <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{OI}, DEBUG_INFO_MISSING_UNRESOLVED{NI}!>+<!> <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>foo<!>(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>getMyProperty<!>())
    var f: String by  <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>foo<!>(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>getMyProperty<!>()) <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{OI}, DEBUG_INFO_MISSING_UNRESOLVED{NI}!>-<!> 1
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
