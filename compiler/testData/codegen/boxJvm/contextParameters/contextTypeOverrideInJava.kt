// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +ContextParameters
// FILE: JavaClass.java
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import org.KotlinInterfaceWithContextType;

public class JavaClass implements KotlinInterfaceWithContextType {
    @Override
    public String foo(Function2<? super String, ? super String, String> a) {
        return a.invoke("O", "K");
    }

    @Override
    public String bar(Function3< ? super String, ? super String, ? super String, String> a) {
        return a.invoke("O", "K", "!");
    }
}

// FILE: KotlinInterfaceWithContextType.kt
package org

interface KotlinInterfaceWithContextType {
    fun foo(a: context(String) (String) -> String): String
    fun bar(a: context(String) String.(String) -> String): String
}

// FILE: test.kt
package org
import JavaClass

fun box(): String {
    if ((JavaClass().foo { a: String, b: String -> a + b } == "OK") &&
        (JavaClass().bar { a: String, b: String, c: String -> a + b + c } == "OK!")) return "OK"
    return "NOK"
}