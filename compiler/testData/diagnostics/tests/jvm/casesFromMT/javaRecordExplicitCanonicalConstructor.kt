// RUN_PIPELINE_TILL: BACKEND
// JDK_KIND: FULL_JDK_17
// JVM_TARGET: 17
// FILE: R.java
// Java record with an explicit canonical constructor. java-direct's source-based finder
// must detect the canonical constructor by parameter-name matching (no PSI available),
// or else FirJavaFacade marks the wrong constructor as primary and component bindings break.
public record R(int a, String b) {
    public R(int a, String b) {
        this.a = a;
        this.b = b == null ? "" : b;
    }
    public static R make(int a, String b) { return new R(a, b); }
}

// FILE: useSite.kt
fun bar(): String {
    val r = R(7, "ok")
    val r2 = R.make(8, null)
    return "${r.a}=${r.b}/${r2.a}=${r2.b}"
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaProperty, javaType, propertyDeclaration, stringTemplate */
