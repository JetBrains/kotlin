// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A(val x: Int, val y: String)

fun foo(block: (A) -> Unit) { }

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Ann

fun bar() {
    foo { (private inline a, <!WRONG_ANNOTATION_TARGET!>@Ann<!> b) ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }
}
