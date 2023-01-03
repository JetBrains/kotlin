// !DIAGNOSTICS: -UNUSED_VARIABLE

external val x: dynamic

var y: Any? by x

fun foo() {
    val a: Any by x
}

class C {
    val a: dynamic by x
}

class A {
    operator fun provideDelegate(host: Any?, p: Any): dynamic = TODO("")
}

val z: Any? by A()

class DynamicHandler {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): dynamic = 23
}

class B {
    val x: dynamic by DynamicHandler()
}
