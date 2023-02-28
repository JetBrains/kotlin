fun notNullString(x: String) {
    js("if (x !== 'abc') throw 'error'")
}

fun nullString(x: String?) {
    js("if (x !== 'abc') throw 'error'")
}

fun null2String(x: String?) {
    js("if (x !== null) throw 'error'")
}

fun testString() {
    notNullString("abc")
    nullString("abc")
    null2String(null)
}

external interface ExternRef

fun notNullExternRef(x: ExternRef) {
    js("if (x !== 'abc') throw 'error'")
}

fun nullExternRef(x: ExternRef?) {
    js("if (x !== 'abc') throw 'error'")
}

fun null2ExternRef(x: ExternRef?) {
    js("if (x !== null) throw 'error'")
}

fun getExternRef(): ExternRef =
    js("'abc'")

fun testExterRef() {
    val externRef = getExternRef()
    notNullExternRef(externRef)
    nullExternRef(externRef)
    null2ExternRef(null)
}

fun notNullInt(x: Int) {
    js("if (x !== 123) throw 'error'")
}

fun nullInt(x: Int?) {
    js("if (x !== 123) throw 'error'")
}

fun null2Int(x: Int?) {
    js("if (x !== null) throw 'error'")
}

fun testInt() {
    notNullInt(123)
    nullInt(123)
    null2Int(null)
}

fun notNullBoolean(x: Boolean) {
    js("if (x !== true) throw 'error'")
}

fun nullBoolean(x: Boolean?) {
    js("if (x !== true) throw 'error'")
}

fun null2Boolean(x: Boolean?) {
    js("if (x !== null) throw 'error'")
}

fun testBoolean() {
    notNullBoolean(true)
    nullBoolean(true)
    null2Boolean(null)
}

fun notNullShort(x: Short) {
    js("x == 123")
}

fun nullShort(x: Short?) {
    js("if (x !== 123) throw 'error'")
}

fun null2Short(x: Short?) {
    js("if (x !== null) throw 'error'")
}

fun testShort() {
    notNullShort(123.toShort())
    nullShort(123.toShort())
    null2Short(null)
}

fun notNullFloat(x: Float) {
    js("if (x !== 123.5) throw 'error'")
}

fun nullFloat(x: Float?) {
    js("if (x !== 123.5) throw 'error'")
}

fun null2Float(x: Float?) {
    js("if (x !== null) throw 'error'")
}

fun testFloat() {
    notNullFloat(123.5f)
    nullFloat(123.5f)
    null2Float(null)
}

fun box(): String {
    testString()
    testExterRef()
    testInt()
    testBoolean()
    testShort()
    testFloat()
    return "OK"
}