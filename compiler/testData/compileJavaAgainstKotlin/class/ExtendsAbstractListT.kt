// FILE: ExtendsAbstractListT.java
package test;

class ExtendsAbstractListT {
    {
        Mine<String> mine = null;
        java.util.List<String> list = mine;
    }
}

// FILE: ExtendsAbstractListT.kt
package test

abstract class Mine<T>() : java.util.AbstractList<T>()
