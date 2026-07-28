// FILE: ClashingSignaturesWithoutReturnType.java
package test;

import java.util.List;

public class ClashingSignaturesWithoutReturnType {
    void test(List<String> ls, List<Integer> li) {
        K k = new K();
        k.foo(ls);
        k.foo(li);
    }
}

// FILE: ClashingSignaturesWithoutReturnType.kt
package test

class K {
    fun foo(l: List<String>): String = ""
    fun foo(l: List<Int>): Int = 1
}
