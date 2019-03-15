// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Id(val id: String)

inline class Name(val name: String)

interface IA {
    fun fromInterface(id: Id)
    fun fromInterface(name: Name)

    fun fromBoth(id: Id)
    fun fromBoth(name: Name)
    
    fun withDefaultImpl(id: Id) {
        if (id.id != "OK") throw AssertionError()
    }

    fun withDefaultImpl(name: Name) {
        if (name.name != "OK") throw AssertionError()
    }
}

abstract class Base {
    abstract fun fromClass(id: Id)
    abstract fun fromClass(name: Name)
    
    abstract fun fromBoth(id: Id)
    abstract fun fromBoth(name: Name)
}


class C : Base(), IA {
    override fun fromInterface(id: Id) {
        if (id.id != "OK") throw AssertionError()
    }

    override fun fromInterface(name: Name) {
        if (name.name != "OK") throw AssertionError()
    }

    override fun fromClass(id: Id) {
        if (id.id != "OK") throw AssertionError()
    }

    override fun fromClass(name: Name) {
        if (name.name != "OK") throw AssertionError()
    }

    override fun fromBoth(id: Id) {
        if (id.id != "OK") throw AssertionError()
    }

    override fun fromBoth(name: Name) {
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