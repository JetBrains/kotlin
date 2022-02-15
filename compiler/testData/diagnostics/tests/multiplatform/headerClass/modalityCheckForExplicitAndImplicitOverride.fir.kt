// MODULE: m1-common
// FILE: common.kt

expect class Foo1 {
    val x: String
}

expect class Foo2 {
    val x: String
}

expect class Foo3 {
    val x: String
}

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

open class Open {
    open val x = "42"
}

actual open class Foo1 : Open() {
    override val x = super.x
}

actual open class Foo2 : Open()

open class WithFinal {
    val x = "42"
}

actual open class Foo3 : WithFinal()
