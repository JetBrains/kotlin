// IGNORE_BACKEND: JS_IR, JS
// WASM_FAILS_IN: SM

@JsFun("(x) => { if (x !== 'abc') throw 'error' }")
external fun notNullString(x: String)

@JsFun("(x) => { if (x !== 'abc') throw 'error' }")
external fun nullString(x: String?)

@JsFun("(x) => { if (x !== null) throw 'error' }")
external fun null2String(x: String?)

fun testString() {
    notNullString("abc")
    nullString("abc")
    null2String(null)
}

external interface ExternRef

@JsFun("(x) => { if (x !== 'abc') throw 'error' }")
external fun notNullExternRef(x: ExternRef)

@JsFun("(x) => { if (x !== 'abc') throw 'error' }")
external fun nullExternRef(x: ExternRef?)

@JsFun("(x) => { if (x !== null) throw 'error' }")
external fun null2ExternRef(x: ExternRef?)

@JsFun("() => 'abc'")
external fun getExternRef(): ExternRef

fun testExterRef() {
    val externRef = getExternRef()
    notNullExternRef(externRef)
    nullExternRef(externRef)
    null2ExternRef(null)
}

class DataRef

@JsFun("(x, y) => { if (x === null) throw 'error' }")
external fun notNullDataRef(x: DataRef)

@JsFun("(x, y) => { if (x === null) throw 'error' }")
external fun nullDataRef(x: DataRef?)

@JsFun("(x, y) => { if (x !== null) throw 'error' }")
external fun null2DataRef(x: DataRef?)

fun testDataRef() {
    val dataRef = DataRef()
    notNullDataRef(dataRef)
    nullDataRef(dataRef)
    null2DataRef(null)
}

@JsFun("(x) => { if (x !== 123) throw 'error' }")
external fun notNullInt(x: Int)

@JsFun("(x) => { if (x !== 123) throw 'error' }")
external fun nullInt(x: Int?)

@JsFun("(x) => { if (x !== null) throw 'error' }")
external fun null2Int(x: Int?)

fun testInt() {
    notNullInt(123)
    nullInt(123)
    null2Int(null)
}

@JsFun("(x) => { if (x !== true) throw 'error' }")
external fun notNullBoolean(x: Boolean)

@JsFun("(x) => { if (x !== true) throw 'error' }")
external fun nullBoolean(x: Boolean?)

@JsFun("(x) => { if (x !== null) throw 'error' }")
external fun null2Boolean(x: Boolean?)

fun testBoolean() {
    notNullBoolean(true)
    nullBoolean(true)
    null2Boolean(null)
}

@JsFun("(x) => { x == 123 }")
external fun notNullShort(x: Short)

@JsFun("(x) => { if (x !== 123) throw 'error' }")
external fun nullShort(x: Short?)

@JsFun("(x) => { if (x !== null) throw 'error' }")
external fun null2Short(x: Short?)

fun testShort() {
    notNullShort(123.toShort())
    nullShort(123.toShort())
    null2Short(null)
}

@JsFun("(x) => { if (x !== 123.5) throw 'error' }")
external fun notNullFloat(x: Float)

@JsFun("(x) => { if (x !== 123.5) throw 'error' }")
external fun nullFloat(x: Float?)

@JsFun("(x) => { if (x !== null) throw 'error' }")
external fun null2Float(x: Float?)

fun testFloat() {
    notNullFloat(123.5f)
    nullFloat(123.5f)
    null2Float(null)
}

@JsFun("(x) => { if (x !== 123.5) throw 'error' }")
external fun notNullNumber(x: Number)

@JsFun("(x) => { if (x !== 123.5) throw 'error' }")
external fun nullNumber(x: Number?)

@JsFun("(x) => { if (x !== null) throw 'error' }")
external fun null2Number(x: Number?)

@JsFun("(x) => { if (x !== 123) throw 'error' }")
external fun byte2Number(x: Number)

@JsFun("(x) => { if (x !== 123) throw 'error' }")
external fun notNullByte2Number(x: Number?)

@JsFun("(x) => { if (x !== null) throw 'error' }")
external fun nullByte2Number(x: Number?)

fun testNumber() {
    notNullNumber(123.5)
    nullNumber(123.5)
    null2Number(null)
    byte2Number(123)
    notNullByte2Number(123)
    nullByte2Number(null)
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