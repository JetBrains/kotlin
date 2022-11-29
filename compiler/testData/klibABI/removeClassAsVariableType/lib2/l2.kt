fun bar() {
    var foo: Foo? = null
    check(foo == null)
}

fun baz() {
    fun qux() {
        var foo: Foo? = null
        check(foo == null)
    }
    qux()
}

fun quux() {
    class Local {
        fun corge() {
            var foo: Foo? = null
            check(foo == null)
        }
    }
    Local().corge()
}

fun grault() {
    object {
        fun garply() {
            var foo: Foo? = null
            check(foo == null)
        }
    }.garply()
}

fun waldo() {
    val fred = object {
        fun garply() {
            var foo: Foo? = null
            check(foo == null)
        }
    }
    fred.garply()
}
