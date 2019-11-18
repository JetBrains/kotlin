// IGNORE_BACKEND_FIR: JVM_IR
// TODO: Enable for JS when it supports Java class library.
// TARGET_BACKEND: JVM

package test

class SomeClass { companion object }

fun box() =
    if ((SomeClass.toString() as java.lang.String).matches("test.SomeClass\\\$Companion@[0-9a-fA-F]+"))
        "OK"
    else
        "Fail: $SomeClass"
