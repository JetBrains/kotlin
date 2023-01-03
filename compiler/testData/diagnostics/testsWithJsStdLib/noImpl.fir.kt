// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER -UNUSED_PARAMETER, -UNREACHABLE_CODE

val prop: String = definedExternally

val prop2: String
    get() = definedExternally

fun foo(x: Int, y: String = definedExternally) {
    println("Hello")
    println("world")

    object {
        fun bar(): Any = definedExternally
    }

    listOf<String>()
            .map<String, String> { definedExternally }
            .filter(fun(x: String): Boolean { definedExternally })

    definedExternally
}

open class A(val x: Int)

open class B() : A(definedExternally) {
    constructor(y: String) : this()

    constructor(y: String, z: String) : this(y + z + definedExternally)
}
