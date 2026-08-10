// FILE: ArrayOfIntegerArray.java
package test;


class ArrayOfIntArray {
    {
        Integer[][] a = new Integer[0][];
        Integer[][] r = ArrayOfIntegerArrayKt.ohMy(a, null);
    }
}

// FILE: ArrayOfIntegerArray.kt
package test

// extra parameter is to make sure generic signature is generated
fun ohMy(p: Array<Array<Int>>, ignore: java.util.List<String>) = p
