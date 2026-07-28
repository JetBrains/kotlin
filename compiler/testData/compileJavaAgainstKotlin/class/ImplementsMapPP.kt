// FILE: ImplementsMapPP.java
package test;


class ImplementsMapPP {
    {
        Mine<String, Integer> mine = null;
        java.util.Map<Integer, String> map = mine;
    }
}

// FILE: ImplementsMapPP.kt
package test

abstract class Mine<P1, P2> : java.util.Map<P2, P1>
