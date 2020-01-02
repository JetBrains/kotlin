// !CHECK_TYPE

fun foo(block: (Int, String) -> Unit) { }
fun foobar(block: (Double) -> Unit) { }

fun bar() {
    foo { _, b ->
        _.hashCode()
        b checkType { _<String>() }
    }

    foo { a, _ ->
        a checkType { _<Int>() }
        _.hashCode()
    }

    foo { _, _ ->
        _.hashCode()
    }

    foo { _: Int, b: String ->
        _.hashCode()
        b checkType { _<String>() }
    }

    foo { a: Int, _: String ->
        a checkType { _<Int>() }
        _.hashCode()
    }

    foo { _: Int, _: String ->
        _.hashCode()
    }

    foo { `_`, _ ->
        _ checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
    }

    foo { _, `_` ->
        _ checkType { _<String>() }
    }

    foo { `_`, `_` ->
        _ checkType { _<String>() }
    }

    foo(fun(x: Int, _: String) {})
}
