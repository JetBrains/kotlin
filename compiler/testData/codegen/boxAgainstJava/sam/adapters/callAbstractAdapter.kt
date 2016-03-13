// FILE: JavaInterface.java

interface JavaInterface {
    void run(Runnable r);
}

// FILE: 1.kt

class Impl: JavaInterface {
    override fun run(r: Runnable?) {
        r?.run()
    }
}

fun box(): String {
    var v = "FAIL"
    Impl().run { v = "OK" }
    return v
}
