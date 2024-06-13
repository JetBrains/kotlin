// TARGET_BACKEND: JVM_IR
// COMMENTED[LANGUAGE: +ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty] uncomment when KT-56386 is fixed
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// Reason: KT-56386 is not fixed yet
// CHECK_BYTECODE_TEXT

// FILE: p/PackagePrivateJavaClass.java
package p;

class PackagePrivateJavaClass {
    public String foo = "OK";
}

// FILE: p/JavaWrapper.java
package p;

public class JavaWrapper {
    protected static class JavaDerived extends PackagePrivateJavaClass {}
}

// FILE: test.kt
import p.JavaWrapper

class KotlinWrapper : JavaWrapper() {
    protected class KotlinDerived : JavaDerived() {
        private val foo = "FAIL"
    }

    fun bar() = KotlinDerived().foo
}

fun box(): String {
    return KotlinWrapper().bar()
}

// 1 GETFIELD p/JavaWrapper\$JavaDerived.foo
