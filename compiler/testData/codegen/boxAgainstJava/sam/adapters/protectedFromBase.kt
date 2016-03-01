// FILE: JavaClass.java

class JavaClass<T> {
    protected void execute(T t, Runnable r) {
        r.run();
    }
}

// FILE: 1.kt

internal class KotlinClass : JavaClass<String>() {
    fun doIt(): String {
        var result = ""
        execute("") {
            result = "OK"
        }
        return result
    }
}

fun box(): String {
    return KotlinClass().doIt()
}
