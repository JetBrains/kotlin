val test1 = { <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>if (true) 1 else ""<!> }

val test2 = { { <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>if (true) 1 else ""<!> } }

val test3: (Boolean) -> Any = { if (it) 1 else "" }

val test4: (Boolean) -> Any? = { if (it) 1 else "" }
