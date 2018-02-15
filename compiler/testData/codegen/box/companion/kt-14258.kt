interface I {
    companion object {
        // getstatic can be used into the getter because field is kept into companion object
        private val bar = "Companion Field from I"
        fun test1(): String {
            // getStatic can be used directly because field is kept into companion object
            return bar
        }

        // getstatic used into the generated accessor
    }
    fun test(): String {
        // accessor is required because field is kept into companion object
        return bar
    }
}

class Foo {
    companion object {
        // accessor is required from getter because field is move to Foo
        private val bar = "Companion Field"
        fun test1(): String {
            // accessor is required because field is move to Foo
            return bar
        }

        private var value = "Value Field"
            set(value) { field = value }
    }

    fun setAndGetValue (): String {
        // setter is required because it is redefined
        value = "Overriden value"
        // getstatic can be used directly because field is move to Foo
        return value
    }

    fun test(): String {
        // getstatic can be used directly because field is move to Foo
        return bar
    }

    // getstatic used into the getter generated accessor for bar
    // getstatic used into the getter generated accessor for value
    // putstatic used into the setter generated accessor for value
}

fun box(): String {
    if (Foo().test() != "Companion Field")
        return "FAILURES"
    if (Foo.test1() != "Companion Field")
        return "FAILURES"
    if (object:I{}.test() != "Companion Field from I")
        return "FAILURES"
    if (I.test1() != "Companion Field from I")
        return "FAILURES"
    if (Foo().setAndGetValue() != "Overriden value")
        return "FAILURES"
    return "OK"
}

