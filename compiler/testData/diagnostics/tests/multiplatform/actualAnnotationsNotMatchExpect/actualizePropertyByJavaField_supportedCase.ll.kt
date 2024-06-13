// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR JVM
// ISSUE: KT-66436

// MODULE: common
// FILE: common.kt
package kotlin.collections

public expect abstract class AbstractMutableList() {
    protected var modCount: Int
}

// MODULE: jvm()()(common)
// FILE: bar/JavaAbstractMutableList.java
package bar; // Java class is in the different package.

public abstract class JavaAbstractMutableList {
    protected transient int modCount = 0;
}

// FILE: jvm.kt
package kotlin.collections

public actual abstract class <!ACTUAL_WITHOUT_EXPECT!>AbstractMutableList<!> <!ACTUAL_WITHOUT_EXPECT!>actual constructor()<!>: bar.JavaAbstractMutableList()
