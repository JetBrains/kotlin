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

    aList.foo { (<!NI;TYPE_MISMATCH, OI;COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>a: String<!>, b) ->
        <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!> <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>checkType<!> { <!NI;UNRESOLVED_REFERENCE!>_<!><Int>() }
        b checkType { _<String>() }
    }

    aList.<!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>foo<!> <!NI;TYPE_MISMATCH!>{ (a, b): B ->
        b checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><Int>() }
        a checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String>() }
    }<!>
}
