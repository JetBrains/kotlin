// RUN_PIPELINE_TILL: BACKEND
// FILE: JavaClass.java

class JavaClass {
    public String getFoo() { return null; }
}

// FILE: test.kt

val x = JavaClass().foo