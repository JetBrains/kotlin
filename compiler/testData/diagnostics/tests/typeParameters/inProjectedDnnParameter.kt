// ISSUE: KT-54663
// FILE: JavaAction.java
import org.jetbrains.annotations.NotNull;

public interface JavaAction<T> {
    void execute(@NotNull T t);
}

// FILE: test.kt
interface KotlinAction<T> {
    fun execute(t: T & Any)
}

class A<T> {
    fun checkJavaIn(element: T, action: JavaAction<in T>) {
        action.execute(element) // OK in K1, type mismatch in K2
    }

    fun checkKotlinIn(element: T, action: KotlinAction<in T>) {
        action.execute(element) // OK in K1, type mismatch in K2
    }

    fun checkJavaInv(element: T, action: JavaAction<T>) {
        action.execute(<!TYPE_MISMATCH!>element<!>) // type mismatch in K1 and K2
    }

    fun checkKotlinInv(element: T, action: KotlinAction<T>) {
        action.execute(<!TYPE_MISMATCH!>element<!>) // type mismatch in K1 and K2
    }
}
