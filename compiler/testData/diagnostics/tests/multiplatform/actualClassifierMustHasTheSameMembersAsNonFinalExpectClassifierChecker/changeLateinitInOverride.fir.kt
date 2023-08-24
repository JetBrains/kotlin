// MODULE: m1-common
// FILE: common.kt

open class Base {
    <!INCOMPATIBLE_MATCHING{JVM}!>open var red1: String = ""<!>
    <!INCOMPATIBLE_MATCHING{JVM}!>open lateinit var red2: String<!>
    open lateinit var green: String
}

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Foo : Base {
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override lateinit var red1: String
    override var red2: String = ""
    override lateinit var green: String
}
