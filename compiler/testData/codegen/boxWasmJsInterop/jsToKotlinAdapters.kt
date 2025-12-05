// WITH_STDLIB

fun notNullString(): String = js("'abc'")

fun nullString(): String? = js("'abc'")

fun null2String(): String? = js("null")

fun testString() {
    check(notNullString() == "abc")
    check(nullString() == "abc")
    check(null2String() == null)
}

external interface ExternRef

fun notNullExternRef(): ExternRef = js("'abc'")

fun nullExternRef(): ExternRef? = js("'abc'")

fun null2ExternRef(): ExternRef? = js("null")


fun testExterRef() {
    check(notNullExternRef() != null)
    check(nullExternRef() != null)
    check(null2ExternRef() == null)
}

class StructRefImpl
typealias StructRef = JsReference<StructRefImpl>

fun notNullStructRef(x: StructRef): StructRef = js("x")

fun nullStructRef(x: StructRef): StructRef? = js("x")

fun null2StructRef(x: StructRef): StructRef? = js("null")

fun testStructRef() {
    val structRef = StructRefImpl().toJsReference()
    check(notNullStructRef(structRef) == structRef)
    check (nullStructRef(structRef) == structRef)
    check (null2StructRef(structRef) == null)
}

fun notNullInt(): Int = js("123")

fun nullInt(): Int? = js("123")

fun null2Int(): Int? = js("null")

fun testInt() {
    check(notNullInt() == 123)
    check(nullInt() == 123)
    check(null2Int() == null)
}

fun notNullBoolean(): Boolean = js("true")

fun nullBoolean(): Boolean? = js("true")

fun null2Boolean(): Boolean? = js("null")

fun testBoolean() {
    check(notNullBoolean() == true)
    check(nullBoolean() == true)
    check(null2Boolean() == null)
}

fun notNullShort(): Short = js("123")

fun nullShort(): Short? = js("123")

fun null2Short(): Short? = js("null")

fun testShort() {
    check(notNullShort() == 123.toShort())
    check(nullShort() == 123.toShort())
    check(null2Short() == null)
}

fun notNullFloat(): Float = js("123.5")

fun nullFloat(): Float? = js("123.5")

fun null2Float(): Float? = js("null")

fun testFloat() {
    check(notNullFloat() == 123.5f)
    check(nullFloat() == 123.5f)
    check(null2Float() == null)
}

fun box(): String {
    testString()
    testExterRef()
    testStructRef()
    testInt()
    testBoolean()
    testShort()
    testFloat()
    return "OK"
}
