// FILE: IntArray.java
package test;


class IntArray {
    {
        int[] r = IntArrayKt.doNothing(new int[0], null);
    }
}

// FILE: IntArray.kt
package test

// extra parameter is to make sure generic signature is not erased
fun doNothing(array: kotlin.IntArray, ignore: java.util.List<String>) = array
