// TARGET_BACKEND: JVM
// FILE: Test.java

interface Test<T> {

    T call();

    default T testDefault(T p) {
        return p;
    }
}

// FILE: main.kt

class Child : Test<String> {
    override fun call() : String {
        return "OK"
    }
}
fun box(): String {
    val res = Child().call()
    if (res != "OK") return "fail $res"

    return Child().testDefault("OK")
}
