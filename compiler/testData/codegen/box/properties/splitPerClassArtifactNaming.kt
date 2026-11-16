// TARGET_BACKEND: JS_IR, JS_IR_ES6
// SPLIT_PER_CLASS
// ES_MODULES

// FILE: a.kt
package sample

interface Handler {
    fun run(): String
}

val topLevelHandler: Handler = object : Handler {
    override fun run() = "top-level-anonymous"
}

class Holder {
    val handler: Handler = object : Handler {
        override fun run() = "from-anonymous"
    }
}

class Foo {
    fun callLocal(): String {
        class Foo {
            val tag = "local-Foo"
        }
        return Foo().tag
    }
}

class `A B` {
    fun name() = "A B"
}

fun makeLocal(): String {
    class Foo {
        val tag = "top-level-local-Foo"
    }
    return Foo().tag
}

// FILE: main.kt
import sample.*

fun box(): String {
    if (topLevelHandler.run() != "top-level-anonymous") return "fail: top-level anonymous object"
    if (makeLocal() != "top-level-local-Foo") return "fail: top-level local class"
    if (Holder().handler.run() != "from-anonymous") return "fail: anonymous object"
    if (Foo().callLocal() != "local-Foo") return "fail: local class Foo"
    if (`A B`().name() != "A B") return "fail: backticked class name"
    return "OK"
}
