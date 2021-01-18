// FILE: JavaInterface.java

public interface JavaInterface {
    String foo(Object... obj);

    String foo(String... str);
}

// FILE: JavaClass.java

public class JavaClass implements JavaInterface {
    public String foo(Object... obj) {
        return "FAIL";
    }

    public String foo(String... str) {
        return "OK";
    }
}

// FILE: test.kt
// TARGET_BACKEND: JVM

class KotlinClass : JavaClass()

fun box(): String {
    return KotlinClass().foo("alpha", "omega")
}
