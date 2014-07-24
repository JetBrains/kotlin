package p

trait T {
    object Null : T { }

    object Other {}
}

fun foo(): T {
    return <caret>
}

// EXIST: { lookupString:"T.Null", itemText:"T.Null", tailText:" (p)", typeText:"T" }
// ABSENT: T.Other
