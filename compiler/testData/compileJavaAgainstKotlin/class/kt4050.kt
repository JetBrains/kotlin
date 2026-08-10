// FILE: kt4050.java
package test;

public class kt4050 {
    public static void main(String[] args) {
        MyEnum.ENTRY.getOrd();
    }
}

// FILE: kt4050.kt
package test

annotation class AAA

enum class MyEnum(@param:AAA @property:Deprecated("") val ord: Int) {
    ENTRY(239);

    fun f(@java.lang.Deprecated p: Int) {

    }
}
