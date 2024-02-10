// MODULE: m1-common
// FILE: common.kt

open class Base {
    open var foo: String = ""
        protected set
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect open class Foo : Base<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override var <!ACTUAL_WITHOUT_EXPECT!>foo<!>: String = ""
        public set
}
