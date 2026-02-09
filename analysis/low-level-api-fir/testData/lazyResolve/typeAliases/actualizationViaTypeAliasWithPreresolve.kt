// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ISSUE: KT-82945
// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// PRE_RESOLVED_PHASE: STATUS
// FILE: MyInterface.kt
package example

interface MyInterface : Closeable {
    override fun cl<caret_preresolved>ose()
}

// FILE: util.kt
package example

public expect interface AutoCloseable {
    public fun close()
}

public expect interface Clo<caret_preresolved>seable : AutoCloseable

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
