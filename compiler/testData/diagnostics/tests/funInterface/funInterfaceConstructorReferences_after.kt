// FIR_IDENTICAL
// !LANGUAGE: +AllowKotlinFunInterfaceConstructorReference +ProhibitJavaSamInterfaceConstructorReference

fun interface Foo {
    fun run()
}

val x = ::Foo
val y = Foo { }
val z = ::<!JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE!>Runnable<!>
