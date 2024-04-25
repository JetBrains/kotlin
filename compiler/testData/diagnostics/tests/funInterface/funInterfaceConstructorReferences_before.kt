// FIR_IDENTICAL
// LANGUAGE: -KotlinFunInterfaceConstructorReference

fun interface Foo {
    fun run()
}

val x = ::<!FUN_INTERFACE_CONSTRUCTOR_REFERENCE!>Foo<!>
val y = Foo { }
val z = ::<!JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE!>Runnable<!>
val w = id(::<!FUN_INTERFACE_CONSTRUCTOR_REFERENCE!>Foo<!>)

fun <T> id(t: T): T = t
