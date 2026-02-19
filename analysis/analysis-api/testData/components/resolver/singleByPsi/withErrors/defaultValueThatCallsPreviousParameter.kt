// Lint regression test from https://issuetracker.google.com/127955232

// FILE: call.kt

package com.example.myapplication

import test.pkg.Context
import test.pkg.R

data class Test(
    val context: Context,
    val testInt: Int,
    val testString: String = context.<expr>getString</expr>(if (testInt == 0) R.string.test_string_1 else R.string.test_string_2))
)

// FILE: Context.java

package test.pkg;

public final class Context {
    public String getString(int id) {
        if (id == R.string.test_string_1) return "id1"
        if (id == R.string.test_string_2) return "id2"
        return "";
    }
}

// FILE: R.java

package test.pkg;

public final class R {
    public static final class string {
        public static final int test_string_1 = 0x7f0a000e;
        public static final int test_string_2 = 0x7f020057;
    }
}
