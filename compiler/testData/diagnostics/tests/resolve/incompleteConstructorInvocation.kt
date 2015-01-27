package p

class X<V>(<!UNUSED_PARAMETER!>provider<!>: () -> V, <!UNUSED_PARAMETER!>trackValue<!>: Boolean) {
}

class B {
    val c = <!NO_VALUE_FOR_PARAMETER!>X<!><String> <!TYPE_MISMATCH!>{
        "e"
    }<!>
}
