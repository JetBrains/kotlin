
// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt
package test.pkg

expect class Foo {
    constructor(withDefault: Int = 0, noDefault: Int)
}

// MODULE: jvm()()(common)
// FILE: main.kt
package test.pkg

actual class Foo {
    actual constructor(with<caret>Default: Int, noDefault: Int)
}
