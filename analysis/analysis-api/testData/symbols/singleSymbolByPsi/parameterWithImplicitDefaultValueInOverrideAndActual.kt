
// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt
package test.pkg

interface Base {
    fun foo(param: Int = 0)
}

expect class Impl : Base {
    override fun foo(param: Int)
}

// MODULE: jvm()()(common)
// FILE: main.kt
package test.pkg

actual class Impl : Base {
    actual override fun foo(pa<caret>ram: Int) {}
}
