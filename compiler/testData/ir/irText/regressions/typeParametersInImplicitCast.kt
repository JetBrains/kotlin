// WITH_STDLIB
// TARGET_BACKEND: JVM
// FILE: ListId.java
import java.util.List;
import org.jetbrains.annotations.NotNull;

class ListId {
    @NotNull
    static <T> List<T> id(List<T> v) {
        return v;
    }
}

// FILE: typeParametersInImplicitCast.kt

fun <T> problematic(lss: List<List<T>>): List<T> = lss.flatMap { ListId.id(it) }
