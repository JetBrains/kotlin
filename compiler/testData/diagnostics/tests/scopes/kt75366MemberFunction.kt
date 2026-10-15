// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-75366
// FILE: A.java
public interface A<T> {
    void foo(T t);
}

// FILE: B.kt
abstract class B<T> : A<T> {
    override fun foo(t: T) {}
    fun bar(t: T) {}
}

// FILE: C.java
public class C extends B<String> {
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
    c.bar("")
    c.foo("")
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, localProperty,
nullableType, override, propertyDeclaration, stringLiteral, typeParameter */
