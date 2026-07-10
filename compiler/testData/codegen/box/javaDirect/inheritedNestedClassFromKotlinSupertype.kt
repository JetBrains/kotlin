// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// Regression test for the java-direct resolution-pipeline collapse: multi-part navigation through
// an *intermediate* segment inherited from a Kotlin (or binary) supertype must be reachable
// through the structural (JavaClass-returning) resolution pipeline. `Local` resolves purely via
// same-file declared lookup (unaffected by the fix, forcing `computeClassifier`'s multi-part loop
// into its `current is JavaClass` branch, which has no `resolve()` fallback), while `Local.Deeper`
// can only be found via the newly-added binary/Kotlin tail of
// `JavaInheritedMemberResolver.findInnerClassFromSupertypes` plus `FirBackedJavaClassAdapter`'s
// newly-implemented `findInnerClass`; `Local.Deeper.EvenDeeper` additionally requires chaining a
// further hop through that same `findInnerClass` on the already-wrapped `Deeper` result.

// FILE: KotlinBase.kt
open class KotlinBase {
    open class Deeper {
        open class EvenDeeper {
            fun value() = "OK"
        }
    }
}

// FILE: JavaBridge.java
public class JavaBridge {
    public static class Local extends KotlinBase {
    }

    // `Deeper` is inherited (not declared) by `Local` from `KotlinBase`, a *different-file Kotlin*
    // supertype — navigating through it is exactly the previously-broken intermediate-segment
    // case; `Deeper.EvenDeeper` additionally chains a further hop through the adapter that `Deeper`
    // itself resolves to.
    public Local.Deeper.EvenDeeper field = new Local.Deeper.EvenDeeper();
}

// FILE: main.kt
fun box(): String {
    return JavaBridge().field.value()
}
