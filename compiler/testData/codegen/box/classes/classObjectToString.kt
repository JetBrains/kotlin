class SomeClass { default object }

fun box() = 
    if ((SomeClass.toString() as java.lang.String).matches("SomeClass\\\$Default@[0-9a-fA-F]+"))
        "OK"
    else
        "Fail: $SomeClass"
