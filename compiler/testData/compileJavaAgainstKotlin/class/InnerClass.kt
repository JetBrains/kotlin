// FILE: InnerClass.java
package test;

class InnerClass {
    Outer.Inner1 field1;
    Outer.Inner2 field2;
}

// FILE: InnerClass.kt
package test

class Outer {
    inner class Inner1
    inner class Inner2(param: String)
}
