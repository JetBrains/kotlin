// FILE: JavaClass.java

class JavaClass {
    private String myX;

    public String getX() {
        return myX;
    }

    public void setX(String x) {
        myX = x;
    }
}

// FILE: 1.kt

fun box(): String {
    return JavaClass().doIt()
}

internal fun JavaClass.doIt(): String {
    x = "OK"
    return x
}
