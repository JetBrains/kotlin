// FILE: call.kt
fun call() {
    val javaClass = JavaClass()
    javaClass.<expr>sub</expr>.foo = 42
}

// FILE: JavaClass.java
class JavaClass {
    private JavaSubClass instance = new JavaSubClass();
    JavaSubClass getSub() { return instance; }
}

// FILE: JavaSubClass.java
class JavaSubClass {
    private int foo = -1;
    int getFoo() { return foo; }
    void setFoo(int v) { foo = v; }
}
