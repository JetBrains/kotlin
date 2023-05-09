// MODULE: m1-common
// FILE: common.kt
expect var v1: Boolean

expect var v2: Boolean
    internal set

expect var v3: Boolean
    internal set

expect open class C {
    var foo: Boolean
}

expect open class C2 {
    var foo: Boolean
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_WITHOUT_EXPECT!>actual var v1: Boolean = false
    private set<!>

actual var v2: Boolean = false

<!ACTUAL_WITHOUT_EXPECT!>actual var v3: Boolean = false
    private set<!>

actual open class C {
    <!ACTUAL_WITHOUT_EXPECT!>actual var foo: Boolean = false
        protected set<!>
}

open class C2Typealias {
    var foo: Boolean = false
        protected set
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>C2<!> = C2Typealias
