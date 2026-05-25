// RUN_PIPELINE_TILL: BACKEND
// FILE: KConsts.kt
@file:JvmName("KConstsKt")
const val KOTLIN_TOP_LEVEL: Int = 41

// FILE: J.java
// Initializer references a Kotlin top-level const through the JVM facade class.
// FirJavaFacade.lazyInitializer needs the resolveInitializerValue callback to evaluate this.
public class J {
    public static final int FROM_KOTLIN = KConstsKt.KOTLIN_TOP_LEVEL + 1;
}

// FILE: useSite.kt
// Use J.FROM_KOTLIN as an annotation argument; this requires const folding at FIR level
// (annotation arguments must be compile-time constants).
@Anno(J.FROM_KOTLIN)
class Use

annotation class Anno(val v: Int)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, javaProperty, propertyDeclaration */
