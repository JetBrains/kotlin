// FIR_IDENTICAL
// KT-7753: attempt to call enum constructor explicitly
enum class A(val c: Int) {
    ONE(1),
    TWO(2),
    THREE(3),
    FORTY_TWO();

    var last: A? = null

    constructor(): this(42) {
        last = <!ENUM_CLASS_CONSTRUCTOR_CALL!>A(13)<!>
    }
}