package p

trait T {
    object Null : T { }

    object Other {}
}

fun foo(): T {
    return <caret>
}

// EXIST: { lookupString:"Null", itemText:"Null", tailText:" (p.T)" }
// EXIST: foo
// EXIST: object
// ABSENT: Other
// NOTHING_ELSE: true
