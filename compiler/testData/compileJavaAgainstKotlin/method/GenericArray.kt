// FILE: GenericArray.java
package test;

class GenericArray {
    public static void ggff() {
        String[] s = GenericArrayKt.ffgg(new String[0]);
    }
}

// FILE: GenericArray.kt
package test

fun <P> ffgg(a: Array<P>) = a
