// FILE: InnerClassOfGeneric.java
package test;

class InnerClassOfGeneric {
    public static void main(String[] args) {
        new Outer<String>().new Inner(new java.util.ArrayList<String>());
        new Outer<String>().new InnerSimple();
    }
}

// FILE: InnerClassOfGeneric.kt
package test

class Outer<T> {
    inner class Inner(list: List<T>)

    inner class InnerSimple()
}
