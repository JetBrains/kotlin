// LANGUAGE: +JvmPermittedSubclassesAttributeForSealed, +FullValueClasses
// ENABLE_JVM_PREVIEW

// FILE: javaExhaustiveWhenOnKotlinSealedClass.kt
sealed value class KS
value class KO(val x: Int) : KS()
class KK() : KS()

fun box(): String =
    J.test(KO(1)) + J.test(KK())

// FILE: J.java
public class J {
    public static String test(KS ks) {
        return switch (ks) {
            case KO ko -> "O";
            case KK kk -> "K";
        };
    }
}
