package p

class X<V>(provider: () -> V, trackValue: Boolean) {
}

class B {
    val c = <!NO_VALUE_FOR_PARAMETER!>X<!><String> <!ARGUMENT_TYPE_MISMATCH!>{
        "e"
    }<!>
}
