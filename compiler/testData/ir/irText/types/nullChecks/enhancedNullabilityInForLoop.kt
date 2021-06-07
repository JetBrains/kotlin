// TARGET_BACKEND: JVM
// FILE: enhancedNullabilityInForLoop.kt
// WITH_JDK

fun use(s: P) {}

fun testForInListUnused() {
    // See KT-36343
    for (x in J.listOfNotNull()) {}
}

fun testForInListDestructured() {
    // See KT-36343 and KT-36344
    for ((x, y) in J.listOfNotNull()) {}
}

fun testDesugaredForInList() {
    val iterator = J.listOfNotNull().iterator()
    while (iterator.hasNext()) {
        val x = iterator.next()
    }
}

fun testForInArrayUnused(j: J) {
    // See KT-36343
    for (x in j.arrayOfNotNull()) {}
}

fun testForInListUse() {
    // See KT-36343
    for (x in J.listOfNotNull()) {
        use(x)
        J.use(x)
    }
}

fun testForInArrayUse(j: J) {
    // See KT-36343
    for (x in j.arrayOfNotNull()) {
        use(x)
        J.use(x)
    }
}

interface K {
    fun arrayOfNotNull(): Array<P>
}

data class P(val x: Int, val y: Int)

// FILE: J.java
import java.util.*;
import org.jetbrains.annotations.*;

public class J implements K {
    public static void use(@NotNull P s) {}

    public static @NotNull P notNull() { return null; }

    public static List<@NotNull P> listOfNotNull() { return null; }

    @Override
    public P[] arrayOfNotNull() { return null; }
}
