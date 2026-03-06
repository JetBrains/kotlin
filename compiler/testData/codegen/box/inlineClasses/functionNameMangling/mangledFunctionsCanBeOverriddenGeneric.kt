// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Id<T: String>(val id: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Name<T: String>(val name: T)

interface IA {
    fun fromInterface(id: Id<String>)
    fun fromInterface(name: Name<String>)

    fun fromBoth(id: Id<String>)
    fun fromBoth(name: Name<String>)
    
    fun withDefaultImpl(id: Id<String>) {
        if (id.id != "OK") throw AssertionError()
    }

    fun withDefaultImpl(name: Name<String>) {
        if (name.name != "OK") throw AssertionError()
    }
}

abstract class Base {
    abstract fun fromClass(id: Id<String>)
    abstract fun fromClass(name: Name<String>)
    
    abstract fun fromBoth(id: Id<String>)
    abstract fun fromBoth(name: Name<String>)
}


class C : Base(), IA {
    override fun fromInterface(id: Id<String>) {
        if (id.id != "OK") throw AssertionError()
    }

    override fun fromInterface(name: Name<String>) {
        if (name.name != "OK") throw AssertionError()
    }

    override fun fromClass(id: Id<String>) {
        if (id.id != "OK") throw AssertionError()
    }

    override fun fromClass(name: Name<String>) {
        if (name.name != "OK") throw AssertionError()
    }

    override fun fromBoth(id: Id<String>) {
        if (id.id != "OK") throw AssertionError()
    }

    override fun fromBoth(name: Name<String>) {
        if (name.name != "OK") throw AssertionError()
    }
}

fun testIA(a: IA) {
    a.fromInterface(Id("OK"))
    a.fromInterface(Name("OK"))

    a.fromBoth(Id("OK"))
    a.fromBoth(Name("OK"))

    a.withDefaultImpl(Id("OK"))
    a.withDefaultImpl(Name("OK"))
}

fun testBase(b: Base) {
    b.fromClass(Id("OK"))
    b.fromClass(Name("OK"))

    b.fromBoth(Id("OK"))
    b.fromBoth(Name("OK"))
}

fun testC(c: C) {
    c.withDefaultImpl(Id("OK"))
    c.withDefaultImpl(Name("OK"))
}

fun box(): String {
    testIA(C())
    testBase(C())
    testC(C())

    return "OK"
}