// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63828

// MODULE: lib
// FILE: Foo.java

import java.util.Set;

public class Foo {
    public interface A extends Set<String> {}

    public interface B extends Set<String> {}
}

// MODULE: main(lib)
// FILE: 1.kt

import Foo.*
import java.util.HashSet

class Impl(b: B): A, B by b

fun box() = "OK"
