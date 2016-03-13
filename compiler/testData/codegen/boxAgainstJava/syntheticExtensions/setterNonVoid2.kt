// FILE: JavaClass.java

class JavaClass {
    private String myX = "";

    public String getX() {
        return myX;
    }

    public double setX(String x) {
        myX = x;
        return 1.0;
    }
}

// FILE: 1.kt

fun box(): String {
    val javaClass = JavaClass()
    if (javaClass.x.isEmpty()) {
        javaClass.x = "OK"
    }
    return javaClass.x
}
