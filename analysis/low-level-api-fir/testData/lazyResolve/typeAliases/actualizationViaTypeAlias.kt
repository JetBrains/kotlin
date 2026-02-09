// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ISSUE: KT-82945
// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: MyInterface.kt
package example

interface MyInterface : Closeable {
    override fun close()
}

// FILE: util.kt
package example

public expect interface AutoCloseable {
    public fun close()
}

public expect interface Closeable : AutoCloseable

// MODULE: jvm()(common)
// FILE: MyClass.kt
package example

class MyClass : MyInterface {
    override public fun clo<caret>se() {}
}

// FILE: util.kt
package example

public actual typealias AutoCloseable = java.lang.AutoCloseable

public actual typealias Closeable = java.io.Closeable
