class T

object A {
    fun T.fooExtension() {}
    val T.fooProperty get() = 10
}

fun usage(t: T) {
    t.foo<caret>
}

// INVOCATION_COUNT: 2
// EXIST: { lookupString: "fooExtension", itemText: "fooExtension" }
// EXIST: { lookupString: "fooProperty", itemText: "fooProperty" }