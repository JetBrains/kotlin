// TARGET_BACKEND: JVM
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB

// Context parameters of companion block members and companion extensions become leading parameters of the statics,
// as for regular context functions. Java passes them explicitly.

// FILE: JavaClient.java
public class JavaClient {
    public static String call() {
        Ctx c = new Ctx("J");
        return C.f(c, 1) + C.getP(c) + I.g(c) + ContextParametersKt.ext(c);
    }
}

// FILE: contextParameters.kt
class Ctx(val v: String)

class C {
    companion {
        context(c: Ctx)
        fun f(x: Int = 0): String = "f${c.v}$x;"

        context(c: Ctx)
        val p: String get() = "p${c.v};"

        // The context is passed implicitly to other members of the block.
        context(c: Ctx)
        fun callsOthers(): String = f(9) + p
    }
}

interface I {
    companion {
        context(c: Ctx)
        fun g(): String = "g${c.v};"
    }
}

context(c: Ctx)
companion fun C.ext(): String = "ext${c.v};"

fun box(): String {
    val kotlin = with(Ctx("K")) { C.f() + C.p + C.callsOthers() + I.g() + C.ext() }
    if (kotlin != "fK0;pK;fK9;pK;gK;extK;") return "Fail Kotlin: $kotlin"

    val java = JavaClient.call()
    if (java != "fJ1;pJ;gJ;extJ;") return "Fail Java: $java"

    return "OK"
}
