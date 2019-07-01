// !LANGUAGE: -ProhibitOperatorMod
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