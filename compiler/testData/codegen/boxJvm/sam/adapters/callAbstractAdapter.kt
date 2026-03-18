// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: JavaInterface.java

interface JavaInterface {
    void run(Runnable r);
}

// MODULE: main(lib)
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
