// TARGET_BACKEND: JS_IR

// FILE: lib.kt
package com.baz
interface Baz {
    val baz1: String
    val baz2: String
    var baz3: String
}

// FILE: lib2.kt
@file:JsExport
package com.baz

class Bay : Baz {
    override val baz1: String
        get() = "baz1"

    override val baz2: String = "baz2"

    override var baz3: String = "baz3"
}

// FILE: main.kt
import com.baz.*
interface Foo {
    val foo: String

    val foo2: String

    var foo3: String
}

@JsExport
class Bar : Foo {
    override val foo: String
        get() = "foo"

    override val foo2: String = "foo2"

    override var foo3: String = "foo3"
}

fun box(): String {
    val bar = Bar()
    if (bar.foo != "foo") return "fail 1"
    if (bar.foo2 != "foo2") return "fail 2"
    if (bar.foo3 != "foo3") return "fail 3"
    bar.foo3 = "foo4"
    if (bar.foo3 != "foo4") return "fail 4"

    val bay = Bay()
    if (bay.baz1 != "baz1") return "fail 5"
    if (bay.baz2 != "baz2") return "fail 6"
    if (bay.baz3 != "baz3") return "fail 7"
    bay.baz3 = "baz4"
    if (bay.baz3 != "baz4") return "fail 8"

    return "OK"
}