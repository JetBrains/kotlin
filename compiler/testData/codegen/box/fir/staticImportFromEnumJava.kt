// TARGET_BACKEND: JVM_IR

// FILE: dependency/Base.java

package dependency;

public enum Base {
    O, K;
}

// FILE: test/test.kt

package test

import dependency.Base.O
import dependency.Base.K

fun box(): String = O.name + K.name
