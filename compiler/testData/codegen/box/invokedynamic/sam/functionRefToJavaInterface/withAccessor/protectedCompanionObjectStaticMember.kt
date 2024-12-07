// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB
// FULL_JDK

// CHECK_BYTECODE_TEXT
// 1 java/lang/invoke/LambdaMetafactory
// 1 c2/C2\.access\$test\$s[0-9]+\(\)Ljava/lang/String\;

// FILE: test.kt
import c2.*

fun box(): String =
    C2().supplier().get()

// FILE: C1.kt
package c1

open class C1 {
    companion object {
        @JvmStatic
        protected fun test(): String = "OK"
    }
}

// FILE: C2.kt
package c2

import c1.*
import j.*

class C2 : C1() {
    fun supplier() = J(::test)
}

// FILE: j/J.java
package j;

public interface J {
    public String get();
}
