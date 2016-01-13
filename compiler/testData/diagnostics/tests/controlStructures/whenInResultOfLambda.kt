val test1 = { <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>when (true) { true -> 1; else -> "" }<!> }

val test2 = { { <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>when (true) { true -> 1; else -> "" }<!> } }

val test3: (Boolean) -> Any = { when (true) { true -> 1; else -> "" } }

val test4: (Boolean) -> Any? = { when (true) { true -> 1; else -> "" } }
