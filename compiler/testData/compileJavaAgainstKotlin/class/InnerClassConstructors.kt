// FILE: InnerClassConstructors.java
package test;

class InnerClassConstructors {
    public static void main(String[] args) {
        new Outer().new InnerGeneric(new java.util.ArrayList<String>());
        new Outer().new InnerPrimitive(1);
    }
}

// FILE: InnerClassConstructors.kt
package test

class Outer {
    inner class InnerGeneric(param: List<String>)

    inner class InnerPrimitive(param: Int)
}
