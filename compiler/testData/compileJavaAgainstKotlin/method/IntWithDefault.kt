// FILE: IntWithDefault.java
package test;


class IntWithDefault {
    {
        int r = IntWithDefaultKt.www(1);
    }
}

// FILE: IntWithDefault.kt
package test

fun www(p: Int = 1) = p
