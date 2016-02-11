// Enable for JS when it supports Java class library.
// TARGET_BACKEND: JVM
class SomeClass { companion object }

fun box() = 
    if ((SomeClass.toString() as java.lang.String).matches("SomeClass\\\$Companion@[0-9a-fA-F]+"))
        "OK"
    else
        "Fail: $SomeClass"
