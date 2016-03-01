// FILE: JavaClass.java

class JavaClass {
    private Runnable r;

    public JavaClass(Runnable r) {
        this.r = r;
    }

    public void run() {
        r.run();
    }
}

// FILE: 1.kt

fun box(): String {
    var v = "FAIL"
    val f = { v = "OK" }
    JavaClass(f).run()
    return v
}
