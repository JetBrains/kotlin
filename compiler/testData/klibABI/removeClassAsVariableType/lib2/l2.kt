fun barRead() {
    var foo: Foo? = null
    check(foo == null)
}

fun barWrite() {
    var foo: Foo?
    foo = null
}

fun bazRead() {
    fun qux() {
        var foo: Foo? = null
        check(foo == null)
    }
    qux()
}

fun bazWrite() {
    fun qux() {
        var foo: Foo?
        foo = null
    }
    qux()
}

fun quuxRead() {
    class Local {
        fun corge() {
            var foo: Foo? = null
            check(foo == null)
        }
    }
    Local().corge()
}

fun quuxWrite() {
    class Local {
        fun corge() {
            var foo: Foo?
            foo = null
        }
    }
    Local().corge()
}

fun graultRead() {
    object {
        fun garply() {
            var foo: Foo? = null
            check(foo == null)
        }
    }.garply()
}

fun graultWrite() {
    object {
        fun garply() {
            var foo: Foo?
            foo = null
        }
    }.garply()
}

fun waldoRead() {
    val fred = object {
        fun garply() {
            var foo: Foo? = null
            check(foo == null)
        }
    }
    fred.garply()
}

fun waldoWrite() {
    val fred = object {
        fun garply() {
            var foo: Foo?
            foo = null
        }
    }
    fred.garply()
}
