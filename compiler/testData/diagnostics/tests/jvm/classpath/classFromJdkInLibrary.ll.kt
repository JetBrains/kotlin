// LL_FIR_DIVERGENCE
// LL resolves `java.util.Date` to the Java source class of the `library` module, which precedes the JDK in the
// module dependencies, while the compiler takes the JDK class first on the classpath.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: library
// FILE: java/util/Date.java
package java.util;

public class Date {
    public static void methodWhichDoesNotExistInJdk() {}
}

// MODULE: main(library)
// FILE: source.kt
import java.util.Date

fun foo() {
    Date.methodWhichDoesNotExistInJdk()
}

/* GENERATED_FIR_TAGS: functionDeclaration */
