// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

open class Base {
    open var foo: Int = 2
        protected set
}
expect class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo : Base() {
    override var foo: Int = 2
        public set
}
