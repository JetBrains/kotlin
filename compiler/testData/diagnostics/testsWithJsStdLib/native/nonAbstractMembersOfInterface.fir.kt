external interface I {
    fun foo(): Unit = definedExternally

    val a: Int?
        get() = definedExternally

    var b: String?
        get() = definedExternally
        set(value) = definedExternally

    val c: Int
        get() = definedExternally

    var d: String
        get() = definedExternally
        set(value) = definedExternally

    var e: dynamic
        get() = definedExternally
        set(value) = definedExternally
}
