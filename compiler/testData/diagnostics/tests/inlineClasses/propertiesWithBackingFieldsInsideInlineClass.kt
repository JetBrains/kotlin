// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -UNUSED_PARAMETER, -INLINE_CLASS_DEPRECATED

interface A {
    val goodSize: Int
}

interface B {
    val badSize: Int
}

inline class Foo(val x: Int) : A, B {
    val a0
        get() = 0

    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val a1<!> = 0

    var a2: Int
        get() = 1
        set(value) {}

    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>var a3: Int<!> = 0
        get() = 1
        set(value) {
            field = value
        }

    override val goodSize: Int
        get() = 0

    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>override val badSize: Int<!> = 0

    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>lateinit var lateinitProperty: String<!>
}
