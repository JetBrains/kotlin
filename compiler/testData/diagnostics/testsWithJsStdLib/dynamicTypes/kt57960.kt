// FIR_IDENTICAL
// FIR_DUMP
// ISSUE: KT-57960

class Bar {
    var toDOM: ((Baz) -> Any)? = null
}

class Baz {
    var text: String = ""
}

object Foo {
    fun create2(
        toDOM: ((Baz) -> Any)? = null,
    ) = jso<Bar> {
        toDOM?.let { this.toDOM = it }
    }
}

private fun buildSchemaNodes1() {
    jso<dynamic> {
        Foo.create2(toDOM = { domNode -> domNode.text })
    }
}


fun <T : Any> jso(): T =
    js("({})")

inline fun <T : Any> jso(
    block: T.() -> Unit,
): T =
    jso<T>().apply(block)
