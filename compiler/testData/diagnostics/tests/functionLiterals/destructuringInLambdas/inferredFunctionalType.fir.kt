// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

fun <T> Iterable<T>.foo(x: (T) -> Unit) {}

fun bar(aList: List<A>) {
    aList.foo { (a, b) ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    aList.foo { (a: Int, b: String) ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    aList.foo { (a, b): A ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    aList.foo { (a: String, b) ->
        a checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        b checkType { _<String>() }
    }

    aList.<!INAPPLICABLE_CANDIDATE!>foo<!> { (a, b): B ->
        b checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        a checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }
}
