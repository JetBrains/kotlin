
// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt
package test.pkg

expect open class Base {
    open fun foo(param: Int = 0)
}

expect class Derived : Base {
    override fun foo(param: Int)
}

// MODULE: jvm()()(common)
// FILE: main.kt
package test.pkg

actual open class Base {
    actual open fun foo(param: Int) {}
}

actual class Derived : Base() {
    actual override fun foo(pa<caret>ram: Int) {}
}
