// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE

external val x: dynamic

var y: Any? by <!PROPERTY_DELEGATION_BY_DYNAMIC!>x<!>

fun foo() {
    val a: Any by <!PROPERTY_DELEGATION_BY_DYNAMIC!>x<!>
}

class C {
    val a: dynamic by <!PROPERTY_DELEGATION_BY_DYNAMIC!>x<!>
}

class A {
    operator fun provideDelegate(host: Any?, p: Any): dynamic = TODO("")
}

val z: Any? by <!PROPERTY_DELEGATION_BY_DYNAMIC!>A()<!>

class DynamicHandler {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): dynamic = 23
}

class B {
    val x: dynamic by DynamicHandler()
}
