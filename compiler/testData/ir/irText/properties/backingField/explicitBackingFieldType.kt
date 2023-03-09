// FIR_IDENTICAL
class A {
    val a = 20

    val it: Number
        field = 4

    var invertedTypes: Int
        field: Number = 42
        get() = if (field.toInt() > 10) field.toInt() else 10

    val p = 5
        get() = field
}
