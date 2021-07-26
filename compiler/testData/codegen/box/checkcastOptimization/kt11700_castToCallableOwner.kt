// TARGET_BACKEND: JVM_IR

// FILE: Parent.java
class Parent<A extends Parent<A>> {
    public A newChild() {
        return (A) new Child();
    }
    public String ok() {
        return "OK";
    }
}

// FILE: Child.java
class Child extends Parent<Child> { }

// FILE: test.kt
import Child
fun box(): String {
    return Child().newChild().ok()
}