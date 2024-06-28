// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS
// SKIP_TXT

// FILE: Test.java
import java.util.Collection;

public class Test {
    static <T> Inv<Collection<? extends T>> bar() {
        return null;
    }
}

// FILE: main.kt
class Inv<E>

fun <R> foo(x: R, y: Inv<R>) {}

fun main() {
    val values: List<Int> = null as List<Int>
    /*
     * Before the fix, there was type mismatch during checking `Test.bar()` to pass to `foo`:
     *      Required: Inv<List<Int>>
     *      Found: Inv<(MutableCollection<out Int!>..Collection<Int!>?)>
     * Constraint `(MutableCollection<out T!>..Collection<T!>?)` from 'Found' (for TypeVariable(R)) has been removed
     * during fixation TypeVariable(T) due to the constraint for R contained TypeVariable(T).
     * The problem was that TypeVariable(T) wan't substituted due to `containsConstrainingTypeWithoutProjection` optimization.
     */
    foo(values, Test.bar())
}