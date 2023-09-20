// MODULE: m1-common
// FILE: common.kt

open class Base {
    <!INCOMPATIBLE_MATCHING{JVM}!>open var foo: String = ""
        protected set<!>
}

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Foo : Base<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

// K2 false negative: KT-61798
actual open class Foo : Base() {
    override var foo: String = ""
        public set
}
