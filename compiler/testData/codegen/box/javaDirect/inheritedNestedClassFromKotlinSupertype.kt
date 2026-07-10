// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// Regression test for the java-direct resolution-pipeline collapse: multi-part navigation through
// an *intermediate* segment inherited from a Kotlin (or binary) supertype must resolve. `Local` is
// a same-file declared class, while `Local.Deeper` can only be found via the binary/Kotlin tail of
// `findInnerClassFromSupertypes` / `resolveInheritedInnerClassToClassId` plus
// `FirBackedJavaClassAdapter`'s `findInnerClass`; `Local.Deeper.EvenDeeper` additionally requires
// chaining a further hop through that same inherited-nested lookup on the already-resolved `Deeper`.

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
