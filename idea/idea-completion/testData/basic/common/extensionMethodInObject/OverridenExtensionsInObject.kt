class T

interface Foo {
    fun T.fooExtension()
    val T.fooProperty: Int
}

object A : Foo {
    override fun T.fooExtension() {}
    override val T.fooProperty get() = 10
}

fun T.usage() {
    foo<caret>
}

// INVOCATION_COUNT: 2
// EXIST: { lookupString: "fooExtension", itemText: "fooExtension" }
// EXIST: { lookupString: "fooProperty", itemText: "fooProperty" }