// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

fun foo(block: (A) -> Unit) { }

fun bar() {
    foo { (_, b) ->
        _.hashCode()
        b checkType { _<String>() }
    }

    foo { (a, _) ->
        a checkType { _<Int>() }
        _.hashCode()
    }

    foo { (_, _) ->
        _.hashCode()
    }

    foo { (_: Int, b: String) ->
        _.hashCode()
        b checkType { _<String>() }
    }

    foo { (a: Int, _: String) ->
        a checkType { _<Int>() }
        _.hashCode()
    }

    foo { (_: Int, _: String) ->
        _.hashCode()
    }

    foo { (_, _): A ->
        _.hashCode()
    }

    foo { (`_`, _) ->
        _ checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
    }

    foo { (_, `_`) ->
        _ checkType { _<String>() }
    }

    foo { (`_`, `_`) ->
        _ checkType { _<String>() }
    }

    foo { (_: String, b) ->
        _.hashCode()
        b checkType { _<String>() }
    }

    foo { (_, b): B ->
        _.hashCode()
        b checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Short>() }
    }
}
