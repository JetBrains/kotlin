// LANGUAGE: +MultiPlatformProjects
// LANGUAGE: +FunctionalTypeWithExtensionAsSupertype
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// MODULE: common
// FILE: common.kt
expect class A: Int.() -> String {
    override fun invoke(p1: Int): String
}

// MODULE: platform()()(common)
// FILE: Java.java
import kotlin.jvm.functions.Function1;

public class Java implements Function1<Integer, String> {
    @Override
    public String invoke(Integer integer) {
        return "OK";
    }
}
// FILE: platform.kt
actual typealias A = Java

fun box() = A()(1)