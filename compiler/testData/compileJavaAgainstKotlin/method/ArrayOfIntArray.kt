// FILE: ArrayOfIntArray.java
package test;


class ArrayOfIntArray {
    {
        int[][] a = new int[0][];
        int[][] r = ArrayOfIntArrayKt.ohMy(a);
    }
}

// FILE: ArrayOfIntArray.kt
package test

fun ohMy(p: Array<IntArray>) = p
