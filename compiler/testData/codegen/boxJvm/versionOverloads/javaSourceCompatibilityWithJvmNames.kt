// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: api.kt

@file:JvmName("VersionedApi")
@file:OptIn(ExperimentalVersionOverloading::class)

@JvmName("javaNamedFunction")
fun kotlinNamedFunction(
    value: String = "O",
    @IntroducedAt("1") suffix: String = "K",
): String = value + suffix

class StaticVersionedOwner {
    companion object {
        @JvmStatic
        fun staticFunction(
            value: String = "O",
            @IntroducedAt("1") suffix: String = "K",
        ): String = value + suffix
    }
}

fun box(): String {
    if (JavaCaller.run() != "OKOK") return "FAIL"
    return "OK"
}

// FILE: JavaCaller.java

public class JavaCaller {
    public static String run() {
        return VersionedApi.javaNamedFunction("O") + StaticVersionedOwner.staticFunction("O");
    }
}
