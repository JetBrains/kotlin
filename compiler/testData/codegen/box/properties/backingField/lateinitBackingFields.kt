// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ExplicitBackingFields

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
