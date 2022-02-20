// WITH_STDLIB
// IGNORE_BACKEND: JVM
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface IFoo {
    fun foo(): String = "K"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcStr<T: String>(val y: T) : IFoo {
    override fun foo(): String = y + super<IFoo>.foo()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcInt<T: Int>(val i: T) : IFoo {
    override fun foo(): String = "O" + super<IFoo>.foo()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcLong<T: Long>(val l: T) : IFoo {
    override fun foo(): String = "O" + super<IFoo>.foo()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcAny<T: Any>(val a: T?) : IFoo {
    override fun foo(): String = "O" + super<IFoo>.foo()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcOverIc<T: IcLong<Long>>(val o: T) : IFoo {
    override fun foo(): String = "O" + super<IFoo>.foo()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcOverSuperInterface<T: IFoo>(val x: T) : IFoo {
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
