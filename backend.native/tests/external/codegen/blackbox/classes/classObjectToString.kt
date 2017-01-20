// TODO: Enable for JS when it supports Java class library.
// IGNORE_BACKEND: JS, NATIVE
class SomeClass { companion object }

fun box() = 
    if ((SomeClass.toString() as java.lang.String).matches("SomeClass\\\$Companion@[0-9a-fA-F]+"))
        "OK"
    else
        "Fail: $SomeClass"
