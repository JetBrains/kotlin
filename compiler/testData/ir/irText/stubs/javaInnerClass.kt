// !DUMP_DEPENDENCIES
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

