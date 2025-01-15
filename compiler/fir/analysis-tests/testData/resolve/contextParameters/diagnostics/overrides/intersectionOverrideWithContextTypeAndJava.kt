// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +ContextParameters

// FILE: JavaInterface.java
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;

public interface JavaInterface {
    String foo(Function2<String, String, String> a);
    String bar(Function3<String, String, String, String> a);
}

// FILE: KotlinInterfaceWithContextType.kt

interface KotlinInterfaceWithContextType {
    fun foo(a: context(String) (String) -> String): String
    fun bar(a: context(String) String.(String) -> String): String
}

// FILE: test.kt
interface IntersectionWithContextType: JavaInterface, KotlinInterfaceWithContextType

fun usage(a: IntersectionWithContextType) {
    a.foo { b: String -> implicit<String>() + b }
    a.bar { b: String -> implicit<String>() + this + b  }
}

context(ctx: T)
fun <T> implicit(): T = ctx