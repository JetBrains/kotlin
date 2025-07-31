// IGNORE_BACKEND_K1: ANY
// FIR_IDENTICAL

// IGNORE_BACKEND_KLIB: JS_IR

class A {
    val a = 20

    val it: Number
        field = 4

    @Suppress("INCONSISTENT_BACKING_FIELD_TYPE", "VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD")
    var invertedTypes: Int
        field: Number = 42
        @Suppress("PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS")
        get() = if (field.toInt() > 10) field.toInt() else 10

    val p = 5
        get() = field
}
