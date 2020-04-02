// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

class TestClass {
    companion object {
        inline operator fun <T> invoke(task: () -> T) = task()
    }
}

fun test(s: String): String {
    val a = TestClass { "K" }
    a checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }

    val b = TestClass { return s }
    b checkType { <!UNRESOLVED_REFERENCE!>_<!><Nothing>() }
}
