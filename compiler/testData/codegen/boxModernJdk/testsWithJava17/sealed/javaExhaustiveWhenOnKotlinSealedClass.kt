// IGNORE_BACKEND: JVM
// LANGUAGE: +JvmPermittedSubclassesAttributeForSealed
// ENABLE_JVM_PREVIEW

// FILE: javaExhaustiveWhenOnKotlinSealedClass.kt
sealed class KS
class KO : KS()
class KK : KS()

fun box(): String =
    J.test(KO()) + J.test(KK())

// FILE: J.java
public class J {
    public static String test(KS ks) {
        return switch (ks) {
            case KO ko -> "O";
            case KK kk -> "K";
        };
    }
}
