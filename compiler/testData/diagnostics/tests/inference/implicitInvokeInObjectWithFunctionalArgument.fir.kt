// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

object TestClass {
    inline operator fun <T> invoke(task: () -> T) = task()
}

fun test(s: String): String {
    val a = <!INAPPLICABLE_CANDIDATE!>TestClass<!> { <!INAPPLICABLE_CANDIDATE!>TestClass<!> { TestClass } }
    a <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><TestClass>() }

    val b = <!INAPPLICABLE_CANDIDATE!>TestClass<!> { return s }
    b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Nothing>() }
}