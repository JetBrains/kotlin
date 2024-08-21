// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_EXPRESSION

inline fun invokeInline(x: () -> Unit, y: Any) {
    x()
}

fun invokeLater(x: () -> Unit, y: Any) {
    x()
}

class A(val k: String)

fun test1() {
    val x: String
    invokeInline(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        try { x = ""; "" } finally { "" }
    )
}

fun test2() {
    val x: String
    invokeLater(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        try { x = ""; "" } finally { "" }
    )
}

fun test3() {
    val x: String
    invokeInline(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        try { "" } catch (e: Exception) { "" } finally { x=""; "" }
    )
}

fun test4() {
    val x: String
    invokeLater(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        try { "" } catch (e: Exception) { "" } finally { x=""; "" }
    )
}

fun test5() {
    val x: String
    invokeInline(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        object {
            init {
                x = ""
            }
        }
    )
}

fun test6() {
    val x: String
    invokeLater(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        object {
            init {
                x = ""
            }
        }
    )
}

fun test7() {
    val x: String
    invokeInline(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        A(if (true) { x = ""; "" } else { x = ""; "" })
    )
}

fun test8() {
    val x: String
    invokeLater(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        A(if (true) { x = ""; "" } else { x = ""; "" })
    )
}