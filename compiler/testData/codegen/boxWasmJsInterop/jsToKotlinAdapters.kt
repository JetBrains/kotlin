// IGNORE_BACKEND: JS_IR, JS

inline fun checkNPE(body: () -> Unit) {
    var throwed = false
    try {
        body()
    } catch (e: NullPointerException) {
        throwed = true
    }
    check(throwed)
}

@JsFun("() => 'abc'")
external fun notNullString(): String

@JsFun("() => null")
external fun notNull2String(): String

@JsFun("() => 'abc'")
external fun nullString(): String?

@JsFun("() => null")
external fun null2String(): String?

fun testString() {
    check(notNullString() == "abc")
    checkNPE { notNull2String() }
    check(nullString() == "abc")
    check(null2String() == null)
}

external interface ExternRef

@JsFun("() => 'abc'")
external fun notNullExternRef(): ExternRef

@JsFun("() => null")
external fun notNull2ExternRef(): ExternRef

@JsFun("() => 'abc'")
external fun nullExternRef(): ExternRef?

@JsFun("() => null")
external fun null2ExternRef(): ExternRef?


fun testExterRef() {
    check(notNullExternRef() != null)
    checkNPE { notNull2ExternRef() }
    check(nullExternRef() != null)
    check(null2ExternRef() == null)
}

class DataRef

@JsFun("(x) => x")
external fun notNullDataRef(x: DataRef): DataRef

@JsFun("(x) => null")
external fun notNull2DataRef(x: DataRef): DataRef

@JsFun("(x) => x")
external fun nullDataRef(x: DataRef): DataRef?

@JsFun("(x) => null")
external fun null2DataRef(x: DataRef): DataRef?

fun testDataRef() {
    val dataRef = DataRef()
    check(notNullDataRef(dataRef) == dataRef)
    checkNPE { notNull2DataRef(dataRef) }
    check (nullDataRef(dataRef) == dataRef)
    check (null2DataRef(dataRef) == null)
}

@JsFun("() => 123")
external fun notNullInt(): Int

@JsFun("() => null")
external fun notNull2Int(): Int

@JsFun("() => 123")
external fun nullInt(): Int?

@JsFun("() => null")
external fun null2Int(): Int?

fun testInt() {
    check(notNullInt() == 123)
    check(notNull2Int() == 0)
    check(nullInt() == 123)
    check(null2Int() == null)
}

@JsFun("() => true")
external fun notNullBoolean(): Boolean

@JsFun("() => null")
external fun notNull2Boolean(): Boolean

@JsFun("() => true")
external fun nullBoolean(): Boolean?

@JsFun("() => null")
external fun null2Boolean(): Boolean?

fun testBoolean() {
    check(notNullBoolean() == true)
    check(notNull2Boolean() == false)
    check(nullBoolean() == true)
    check(null2Boolean() == null)
}

@JsFun("() => 123")
external fun notNullShort(): Short

@JsFun("() => null")
external fun notNull2Short(): Short

@JsFun("() => 123")
external fun nullShort(): Short?

@JsFun("() => null")
external fun null2Short(): Short?

fun testShort() {
    check(notNullShort() == 123.toShort())
    check(notNull2Short() == 0.toShort())
    check(nullShort() == 123.toShort())
    check(null2Short() == null)
}

@JsFun("() => 123.5")
external fun notNullFloat(): Float

@JsFun("() => null")
external fun notNull2Float(): Float

@JsFun("() => 123.5")
external fun nullFloat(): Float?

@JsFun("() => null")
external fun null2Float(): Float?

fun testFloat() {
    check(notNullFloat() == 123.5f)
    check(notNull2Float() == 0.0f)
    check(nullFloat() == 123.5f)
    check(null2Float() == null)
}


@JsFun("() => 123.5")
external fun notNullNumber(): Number

@JsFun("() => null")
external fun notNull2Number(): Number

@JsFun("() => 123.5")
external fun nullNumber(): Number?

@JsFun("() => null")
external fun null2Number(): Number?

fun testNumber() {
    check(notNullNumber() == 123.5)
    check(notNull2Number() == 0.0)
    check(nullNumber() == 123.5)
    check(null2Number() == null)
}

fun box(): String {
    testString()
    testExterRef()
    testDataRef()
    testInt()
    testBoolean()
    testShort()
    testFloat()
    testNumber()
    return "OK"
}
