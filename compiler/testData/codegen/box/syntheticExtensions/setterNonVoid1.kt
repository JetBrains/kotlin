// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    private String myX = "";

    public String getX() {
        return myX;
    }

    public JavaClass setX(String x) {
        myX = x;
        return this;
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    val javaClass = JavaClass()
    if (javaClass.x == "") {
        javaClass.x = "OK"
    }
    return javaClass.x
}
