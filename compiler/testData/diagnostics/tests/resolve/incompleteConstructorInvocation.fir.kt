package p

class X<V>(provider: () -> V, trackValue: Boolean) {
}

class B {
    val c = <!INAPPLICABLE_CANDIDATE!>X<!><String> {
        "e"
    }
}
