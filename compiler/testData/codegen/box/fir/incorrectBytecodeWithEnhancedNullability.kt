// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FILE: Utils.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Utils {
    @NotNull
    public static A resolveVisibilityFromModifiers(@NotNull A defaultVisibility) {
        return A.Companion.getPUBLIC();
    }

    @Nullable
    public static Integer compare(@NotNull A first, @NotNull A second) {
        if (first.getIndex() == second.getIndex()) return null;
        return Integer.compare(first.getIndex(), second.getIndex());
    }
}

// FILE: main.kt

class KtModifierListOwner
class A(val index: Int) {
    companion object {
        val PUBLIC = A(1)
        val PRIVATE = A(2)
    }
}

fun test(visibility: A): String {
    val parentVisibility = Utils.resolveVisibilityFromModifiers(visibility)
    if (Utils.compare(parentVisibility, visibility) ?: 0 < 0) {
        return "OK"
    }
    return "Fail"
}

fun box(): String {
    return test(A.PRIVATE)
}
