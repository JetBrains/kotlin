// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81558, KT-89232
// FIR_DUMP

interface Provider<T> {
    fun get(): T
}

interface Property<T> : Provider<T> {
    fun set(t: T?)
    fun set(provider: Provider<out T?>)
}

fun <T> Property<T>.assign(value: T?) {}
fun <T> Property<T>.assign(provider: Provider<out T?>) {}

abstract class PropertyRepro<T> {
    abstract var fooPlainProperty: Foo
    abstract fun acceptFoo(foo: Foo)
    abstract fun acceptNullableFoo(foo: Foo?)
    abstract fun acceptGeneric(t: T)

    abstract val foo: Property<Foo>
}

enum class Foo {
    BAR, BAZ
}

fun propertyRepro(propertyRepro: PropertyRepro<Foo>, provider: Provider<Foo>) {
    propertyRepro.fooPlainProperty = BAR
    propertyRepro.acceptFoo(BAR)
    propertyRepro.acceptNullableFoo(BAR)
    propertyRepro.acceptGeneric(BAR)

    propertyRepro.foo.set(Foo.BAR)
    propertyRepro.foo.set(provider)
    propertyRepro.foo.assign(Foo.BAR)

    propertyRepro.foo.set(BAR)
    propertyRepro.foo.assign(BAR)
    propertyRepro.foo.set(<!UNRESOLVED_REFERENCE!>UNRESOLVED<!>)
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, enumDeclaration, enumEntry, funWithExtensionReceiver,
functionDeclaration, interfaceDeclaration, nullableType, outProjection, propertyDeclaration, typeParameter */
