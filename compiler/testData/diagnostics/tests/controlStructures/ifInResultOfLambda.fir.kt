// !WITH_NEW_INFERENCE
val test1 = { if (true) 1 else "" }

val test2 = { { if (true) 1 else "" } }

val test3: (Boolean) -> Any = { if (it) 1 else "" }

val test4: (Boolean) -> Any? = { if (it) 1 else "" }
