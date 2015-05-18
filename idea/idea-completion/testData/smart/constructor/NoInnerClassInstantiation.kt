class C {
    inner class Inner
}

fun foo() : C.Inner {
    return <caret>
}

// ABSENT: Inner
// EXIST: foo
// NOTHING_ELSE
