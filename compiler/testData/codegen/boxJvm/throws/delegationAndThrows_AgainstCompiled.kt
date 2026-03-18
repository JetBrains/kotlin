// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: A.java

import java.io.IOException;

public interface A {
    void foo() throws IOException;
}

// MODULE: main(lib)
// FILE: B.kt

class B(a: A) : A by a

fun box(): String {
    val method = B::class.java.declaredMethods.single { it.name == B::foo.name }
    if (method.exceptionTypes.size != 0)
        return "Fail: ${method.exceptionTypes.toList()}"

    return "OK"
}
