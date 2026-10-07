// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: Ann.kt
package test

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val value: Scope) {
    enum class Scope {
        A,
        B,
    }
}

// MODULE: main(lib)
// FILE: LocalAnn.kt
package test

@Retention(AnnotationRetention.RUNTIME)
annotation class LocalAnn(val value: Scope) {
    enum class Scope {
        A,
        B,
    }
}

// FILE: test/JavaInterface.java
package test;

import test.Ann.Scope;
import static test.Ann.Scope.A;

public interface JavaInterface {
    @Ann(Scope.B)
    @LocalAnn(LocalAnn.Scope.B)
    String imported();

    @Ann(Ann.Scope.B)
    String qualified();

    @Ann(A)
    String staticImport();
}

// FILE: test/LocalJavaInterface.java
package test;

import test.LocalAnn.Scope;
import static test.LocalAnn.Scope.A;

public interface LocalJavaInterface {
    @LocalAnn(Scope.B)
    String imported();

    @LocalAnn(A)
    String staticImport();
}

// FILE: Delegator.kt
package test

class Delegator(impl: JavaInterface) : JavaInterface by impl
class LocalDelegator(impl: LocalJavaInterface) : LocalJavaInterface by impl

fun box(): String {
    val d = Delegator(object : JavaInterface {
        override fun imported(): String = "O"
        override fun qualified(): String = ""
        override fun staticImport(): String = ""
    })
    val ld = LocalDelegator(object : LocalJavaInterface {
        override fun imported(): String = "K"
        override fun staticImport(): String = ""
    })

    val m1 = Delegator::class.java.getMethod("imported")
    if (m1.getAnnotation(Ann::class.java)?.value != Ann.Scope.B) return "Fail m1 Ann"
    if (m1.getAnnotation(LocalAnn::class.java)?.value != LocalAnn.Scope.B) return "Fail m1 LocalAnn"

    val m2 = Delegator::class.java.getMethod("qualified")
    if (m2.getAnnotation(Ann::class.java)?.value != Ann.Scope.B) return "Fail m2"

    val m3 = Delegator::class.java.getMethod("staticImport")
    if (m3.getAnnotation(Ann::class.java)?.value != Ann.Scope.A) return "Fail m3"

    val lm1 = LocalDelegator::class.java.getMethod("imported")
    if (lm1.getAnnotation(LocalAnn::class.java)?.value != LocalAnn.Scope.B) return "Fail lm1"

    val lm2 = LocalDelegator::class.java.getMethod("staticImport")
    if (lm2.getAnnotation(LocalAnn::class.java)?.value != LocalAnn.Scope.A) return "Fail lm2"

    return d.imported() + ld.imported()
}
