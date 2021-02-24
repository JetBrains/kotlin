// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

val test1 = { if (true) <!IMPLICIT_CAST_TO_ANY{OI}!>1<!> else <!IMPLICIT_CAST_TO_ANY{OI}!>""<!> }

val test2 = { { if (true) <!IMPLICIT_CAST_TO_ANY{OI}!>1<!> else <!IMPLICIT_CAST_TO_ANY{OI}!>""<!> } }

val test3: (Boolean) -> Any = { if (it) 1 else "" }

val test4: (Boolean) -> Any? = { if (it) 1 else "" }
