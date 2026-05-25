// RUN_PIPELINE_TILL: BACKEND
// FILE: KSuper.kt
open class KSuper {
    open class Inner(val name: String)
}

// FILE: J.java

// References "Inner" by simple name; resolution must walk up to the Kotlin supertype.
public class J extends KSuper {
    public static class JInner extends Inner {
        public JInner(String s) { super(s); }
    }
}

// FILE: useSite.kt
// Force Kotlin to verify JInner's supertype chain reaches KSuper.Inner.
fun upcast(x: J.JInner): KSuper.Inner = x

fun bar(): String = upcast(J.JInner("ok")).name

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaFunction, primaryConstructor, propertyDeclaration */
