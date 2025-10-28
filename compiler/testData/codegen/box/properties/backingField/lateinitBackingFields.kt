// LANGUAGE: +ExplicitBackingFields
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

@Suppress("INCONSISTENT_BACKING_FIELD_TYPE", "VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD")
var that: Int
    @Suppress("WRONG_MODIFIER_TARGET")
    lateinit field: String
    @Suppress("PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS")
    get() = field.length
    @Suppress("PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS")
    set(value) {
        field = value.toString()
    }

fun box(): String {
    that = 1

    return if (that == 1) {
        "OK"
    } else {
        "fail: $that"
    }
}
