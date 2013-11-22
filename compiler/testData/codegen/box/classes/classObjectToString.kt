class SomeClass { class object }

fun box() = 
    if ((SomeClass.toString() as java.lang.String).matches("SomeClass\\\$object@[0-9a-fA-F]+"))
        "OK"
    else
        "Fail: $SomeClass"
