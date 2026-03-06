// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

interface IFoo {
    fun foo(): String = "K"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcStr(val y: String) : IFoo {
    override fun foo(): String = y + super<IFoo>.foo()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcInt(val i: Int) : IFoo {
    override fun foo(): String = "O" + super<IFoo>.foo()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcLong(val l: Long) : IFoo {
    override fun foo(): String = "O" + super<IFoo>.foo()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcAny(val a: Any?) : IFoo {
    override fun foo(): String = "O" + super<IFoo>.foo()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcOverIc(val o: IcLong) : IFoo {
    override fun foo(): String = "O" + super<IFoo>.foo()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcOverSuperInterface(val x: IFoo) : IFoo {
    override fun foo(): String = "O" + super<IFoo>.foo()
}

fun check(message: String, iFoo: IFoo) {
    val actual = iFoo.foo()
    if (actual != "OK")
        throw Exception("$message: \"$actual\" != OK")
}

fun box(): String {
    check("IcStr", IcStr("O"))
    check("IcInt", IcInt(42))
    check("IcLong", IcLong(42L))
    check("IcAny", IcAny(""))
    check("IcOverIc", IcOverIc(IcLong(42L)))
    check("IcOverSuperInterface", IcOverSuperInterface(IcInt(42)))

    return "OK"
}
