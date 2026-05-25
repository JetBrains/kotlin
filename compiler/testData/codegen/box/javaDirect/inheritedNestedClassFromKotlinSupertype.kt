// TARGET_BACKEND: JVM_IR

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
