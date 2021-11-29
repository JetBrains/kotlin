// FIR_IDENTICAL
// !LANGUAGE: -AllowKotlinFunInterfaceConstructorReference -ProhibitJavaSamInterfaceConstructorReference

fun interface Foo {
    fun run()
}

val x = ::<!FUN_INTERFACE_CONSTRUCTOR_REFERENCE!>Foo<!>
val y = Foo { }
val z = ::Runnable