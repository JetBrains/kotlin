// FILE: IntegerArray.java
package test;


class IntArray {
    {
        Integer[] r = IntegerArrayKt.doNothing(new Integer[0], null);
    }
}

// FILE: IntegerArray.kt
package test

// extra parameter is to make sure generic signature is preserved
fun doNothing(array: Array<Int>, ignore: java.util.List<String>) = array
