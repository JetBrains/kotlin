// !LANGUAGE: +NestedClassesInAnnotations
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// TARGET_BACKEND: JVM
// FILE: Foo.java

@Anno(Anno.CONST)
public class Foo {}

// FILE: Anno.kt

annotation class Anno(val value: Int) {
    companion object {
        const val CONST = 42
    }
}

fun box(): String =
        if ((Foo::class.java.annotations.single() as Anno).value == 42) "OK" else "Fail"

