// TARGET_BACKEND: JVM
// !LANGUAGE: -ProhibitOperatorMod
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6
// FIR status: don't support legacy feature
// MODULE: lib
// FILE: Java.java

import org.jetbrains.annotations.NotNull;

class AugmentedAssignmentPure {
    void modAssign(Runnable i) {
        i.run();
    }
}

class AugmentedAssignmentViaSimpleBinary {
    @NotNull AugmentedAssignmentViaSimpleBinary rem(Runnable i) {
        i.run();
        return this;
    }
}

class Binary {
    Binary rem(Runnable i) {
        i.run();
        return this;
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    val augAssignmentPure = AugmentedAssignmentPure()
    var v1 = "FAIL"
    augAssignmentPure %= { v1 = "OK" }
    if (v1 != "OK") return "assignment pure: $v1"

    var augmentedAssignmentViaSimpleBinary = AugmentedAssignmentViaSimpleBinary()
    var v2 = "FAIL"
    augmentedAssignmentViaSimpleBinary %= { v2 = "OK" }
    if (v2 != "OK") return "assignment via simple binary: $v2"

    val binary = Binary()
    var v3 = "FAIL"
    binary % { v3 = "OK" }
    if (v3 != "OK") return "binary: $v3"

    return "OK"
}
