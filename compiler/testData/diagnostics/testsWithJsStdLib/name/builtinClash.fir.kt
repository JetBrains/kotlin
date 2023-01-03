class C {
    class prototype

    class length

    class `$metadata$`

    fun constructor() {}
}

class D {
    private class prototype

    private class length

    private class `$metadata$`

    private fun constructor() {}
}

class E {
    @JsName("prototype")
    class D

    @JsName("constructor")
    fun f() {}
}

class F {
    @JsName("A")
    class prototype

    @JsName("B")
    class length

    @JsName("f")
    fun constructor() {}
}

class prototype

class length

fun constructor() {
}

fun f() {
    class prototype
    class length

    fun constructor() {}
}

external interface Object {
    val constructor: Any?
}
