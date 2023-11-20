// MODULE: m1-common
// FILE: common.kt

open class Base {
    open var red1: String = ""
    open lateinit var red2: String
    open lateinit var green: String
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect open class Foo : Base {
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override lateinit var <!ACTUAL_WITHOUT_EXPECT!>red1<!>: String
    override var <!ACTUAL_WITHOUT_EXPECT!>red2<!>: String = ""
    override lateinit var green: String
}
