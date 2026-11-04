// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: J
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib reads Java declarations from compiled classes, not from sources.
// FILE: J.java

public class J {
    public class JInner {
        public void foo() {}
        public int z = 0;
    }
    public void bar() {}
    public int x = 0;
}

// FILE: javaInnerClass.kt

class Test1 : J() {
    val test = JInner()
}

