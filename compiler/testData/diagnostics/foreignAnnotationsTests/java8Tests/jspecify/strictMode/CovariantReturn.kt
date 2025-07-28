// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// ISSUE: KT-78541

// FILE: p/Sub.java
package p;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Sub<T extends @Nullable Object> implements Super<T> {
    private T t;

    public Sub(T t) {
        this.t = t;
    }

    @Override
    public Sub<T> self() {
        return this;
    }

    public T get() {
        return t;
    }

    public void set(T t) {
        this.t = t;
    }
}

// FILE: p/Super.java
package p;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface Super<T extends @Nullable Object> {
    Super<T> self();
}

// FILE: p/K.kt
package p

fun <T : Any> test(t: T) {
    val s = Sub<T>(t)
    <!DEBUG_INFO_EXPRESSION_TYPE("p.Sub<(T..T?)>")!>s.self()<!>.set(null)
    <!DEBUG_INFO_EXPRESSION_TYPE("T")!>s.get()<!>
}
