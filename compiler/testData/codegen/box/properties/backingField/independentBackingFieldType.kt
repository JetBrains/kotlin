// LANGUAGE: +ExplicitBackingFields
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

fun createString() = "AAA" + "BBB"

@Suppress("INCONSISTENT_BACKING_FIELD_TYPE")
class A {
    @Suppress("INCONSISTENT_BACKING_FIELD_TYPE", "PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS", "VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD")
    var it: Int
        field = 3.14
        get() = (field + 10).toInt()
        set(value) {
            field = (value - 10).toDouble()

            if (field < -3 || -1 < field) {
                throw Exception("fail: value = $value, field = $field")
            }
        }

    @Suppress("INCONSISTENT_BACKING_FIELD_TYPE", "PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS", "VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD")
    var that: Int
        field = createString() + "!"
        get() = field.length
        set(value) {
            field = value.toString()

            if (field != "17") {
                throw Exception("fail: value = $value, field = $field")
            }
        }
}

fun box(): String {
    try {
        val a = A()

        val it: Int = A().it and 10
        a.it = it

        val that: Int = a.that
        a.that = that + 10
    } catch (e: Exception) {
        return e.message ?: "fail"
    }
    return "OK"
}
