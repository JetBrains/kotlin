// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

class TestClass {
    companion object {
        inline operator fun <T> invoke(task: () -> T) = task()
    }
}

fun test(s: String): String {
    val a = <!INAPPLICABLE_CANDIDATE!>TestClass<!> { "K" }
    a <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }

    val b = <!INAPPLICABLE_CANDIDATE!>TestClass<!> { return s }
    b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Nothing>() }
}