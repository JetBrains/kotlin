// JAVAC_EXPECTED_FILE
// LANGUAGE: +TypeEnhancementImprovementsInStrictMode

package test;

import org.jetbrains.annotations.*;

/*
 * Note that a receiver type doesn't get into signatures used by the Kotlin compiler
 * So in this test, annotated types shouldn't be reflected in the signatures dump
 */

public class MethodReceiver<T> {
    public void f1(MethodReceiver<@Nullable T> this) { }

    class MethodReceiver3<T, K, L> {
        public void f1(@Nullable MethodReceiver3<@Nullable T, K, @Nullable L> this) { }
    }
}