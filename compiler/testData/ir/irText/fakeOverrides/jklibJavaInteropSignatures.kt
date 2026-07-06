// FIR_IDENTICAL
// TARGET_BACKEND: JKLIB
// SKIP_KT_DUMP
// FILE: jklibJavaInteropSignatures.kt
package test

import java.util.HashMap
import java.util.function.BiFunction

// ============================================================================
// Scenario 1: Mapped JRE Collection Methods (isMappedJreClassMember)
// ============================================================================
// VERIFY: In the generated .ir.txt dump, calling 'merge' on HashMap must resolve to its JVM signature
// (e.g., merge(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;)
// rather than a Kotlin K2 mangled signature. If it received a Kotlin signature, J2CL linking against
// jre.jar would fail with an unbound symbol error.
fun testMapMerge(map: HashMap<String, String>, remapper: BiFunction<in String, in String, out String>) {
    map.merge("key", "val", remapper)
}

// ============================================================================
// Scenario 2: Any and Throwable in Java-Backed Hierarchies (isAnyOrThrowable)
// ============================================================================
// VERIFY: In the IR dump, overriding equals, hashCode, and toString on a Kotlin class subclassing a Java
// base class must retain KOTLIN signatures (not JVM signatures like hashCode()I or toString()Ljava/lang/String;).
// Why: The Kotlin compiler expects built-in Any/Throwable methods to ALWAYS use Kotlin signatures in KLib metadata.
class KotlinSubClass : JavaBase() {
    override fun hashCode(): Int = 42
    override fun toString(): String = "KotlinSubClass"
    override fun equals(other: Any?): Boolean = other is KotlinSubClass
}

// ============================================================================
// Scenario 3: Fake Overrides of Java Methods (resolveFakeOverrideMaybeAbstract)
// ============================================================================
// VERIFY: In the IR dump, calling the fake override 'javaMethod' on KotlinSubClass must resolve to its
// real Java target (JavaBase.javaMethod) and receive the JVM signature of that target.
fun testFakeOverride(sub: KotlinSubClass) {
    sub.javaMethod()
}

// FILE: JavaBase.java
package test;

public class JavaBase {
    public String javaMethod() { return "java"; }
    @Override public int hashCode() { return 1; }
    @Override public String toString() { return "base"; }
}
