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

var status: String = "fail"  // global property to avoid issues with accessing closure from local class (KT-4174)

fun box(): String {
    class C() : JavaClass({status = "OK"}) {}
    C().run()
    return status
}
