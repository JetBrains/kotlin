// !WITH_NEW_INFERENCE
val test1 = { if (true) <!OI;IMPLICIT_CAST_TO_ANY!>1<!> else <!OI;IMPLICIT_CAST_TO_ANY!>""<!> }

val test2 = { { if (true) <!OI;IMPLICIT_CAST_TO_ANY!>1<!> else <!OI;IMPLICIT_CAST_TO_ANY!>""<!> } }

val test3: (Boolean) -> Any = { if (it) 1 else "" }

val test4: (Boolean) -> Any? = { if (it) 1 else "" }
