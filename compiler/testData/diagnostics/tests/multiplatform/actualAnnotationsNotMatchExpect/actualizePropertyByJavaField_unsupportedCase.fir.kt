// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// ISSUE: KT-66436

// MODULE: common
// FILE: common.kt
package foo

public <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> abstract class AbstractMutableList() {
    protected var <!EXPECT_ACTUAL_MISMATCH{JVM}!>modCount<!>: Int
}

// MODULE: jvm()()(common)
// FILE: bar/JavaAbstractMutableList.java
package bar; // Java class is in the different package.

public abstract class JavaAbstractMutableList {
    protected transient int modCount = 0;
}

// FILE: jvm.kt
package foo

public actual abstract class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>AbstractMutableList<!> actual constructor(): bar.JavaAbstractMutableList()
