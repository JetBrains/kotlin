package p

class Outer {
    trait T {
        object Null : T { }
    }
}

fun foo(): Outer.T {
    return <caret>
}

// ELEMENT: "T.Null"
