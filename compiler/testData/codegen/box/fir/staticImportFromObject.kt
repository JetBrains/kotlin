// TARGET_BACKEND: JVM_IR
// ISSUE: KT-58980

// FILE: dependency/Base.java

package dependency;

public class Base {
    public String string = "OK";
}

// FILE: test/test.kt

package test

import dependency.Base
import test.KtObject.string

object KtObject : Base()

fun box(): String = string
