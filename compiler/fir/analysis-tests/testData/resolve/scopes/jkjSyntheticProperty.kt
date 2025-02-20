// RUN_PIPELINE_TILL: BACKEND
// SCOPE_DUMP: C:foo;getFoo
// LANGUAGE: +MultiPlatformProjects
// IGNORE_REVERSED_RESOLVE
// ISSUE: KT-75323
// FILE: A.java
public interface A {
    String getFoo();
}

// FILE: B.kt
open class B : A {
    override fun getFoo(): String = foo
}

// FILE: C.java
public class C extends B {
    public static C create() {
        return null;
    }

    public static C create(String str) {
        return null;
    }
}

// FILE: main.kt
fun main() {
    val c = C.create()
    c.foo
}
