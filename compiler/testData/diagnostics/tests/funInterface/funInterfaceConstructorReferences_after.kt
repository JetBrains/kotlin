// FIR_IDENTICAL
// LANGUAGE: +KotlinFunInterfaceConstructorReference

fun interface Foo {
    fun run()
}

val x = ::Foo
val y = Foo { }
val z = ::<!JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE!>Runnable<!>
val w = id(::Foo)

fun <T> id(t: T): T = t
