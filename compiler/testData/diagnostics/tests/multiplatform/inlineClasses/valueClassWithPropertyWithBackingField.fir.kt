// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class A(val i: Int)

expect value class B(val i: Int) {
    var x: Int
    var y: Int
}

expect value class C(val i: Int)

interface I {
    val x: Int
}

// MODULE: jvm()()(common)
// FILE: jvm.kt
@JvmInline
actual value class A(val i: Int) {
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val x<!> = i
    val y
        get() = i
}

@JvmInline
actual value class B(val i: Int) {
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>actual var x<!> = 0
        get() = i
        set(value) {
            field = value
        }

    actual var y: Int
        get() = i
        set(value) {}
}

@JvmInline
actual value class C(val i: Int) : I {
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>override val x: Int<!> = i
}
