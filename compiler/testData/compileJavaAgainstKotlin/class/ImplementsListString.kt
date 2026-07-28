// FILE: ImplementsListString.java
package test;


class PlainExtendsListString {
    {
        Mine mine = null;
        java.util.List<String> list = mine;
    }
}

// FILE: ImplementsListString.kt
package test

abstract class Mine : java.util.List<String>
