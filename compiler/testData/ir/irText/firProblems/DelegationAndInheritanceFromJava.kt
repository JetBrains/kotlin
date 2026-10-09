// DUMP_IR_DIFFERENCE: JKLIB
// TARGET_BACKEND: JVM
// SKIP_IR_DESERIALIZATION_CHECKS
// ^Contains the additional:
//  ```
//  annotations:
//    Deprecated(1 = "This member is not fully supported by Kotlin compiler, so it may be absent or have different signature in next major version", 2 = ReplaceWith(1 = "", 2 = [] type=kotlin.Array<out kotlin.String> varargElementType=kotlin.String), 3 = DeprecationLevel.WARNING)
//  ```
//  for `Impl.toArray`

// FILE: Foo.java

import java.util.Set;

public class Foo {
    public interface A extends Set<String> {}

    public interface B extends Set<String> {}
}

// FILE: DelegationAndInheritanceFromJava.kt

import Foo.*
import java.util.HashSet

class Impl(b: B): A, B by b

fun box() = "OK"
