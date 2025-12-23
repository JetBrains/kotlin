// JSPECIFY_STATE: strict
// ISSUE: KT-83314

// FILE: BoxBase.java
import org.jspecify.annotations.*;

@NullMarked
public class BoxBase<T extends @Nullable Object> {
    public void set(T t) {}
    public T get() { return null;}
}

// FILE: BoxDerived.java
import org.jspecify.annotations.*;

@NullMarked
public class BoxDerived<E extends @Nullable Object> extends BoxBase<E> {
}

// FILE: Base.java

import org.jspecify.annotations.*;

@NullMarked
public class Base {
    public BoxBase<@Nullable String> foo() { return null; }
}

// FILE: Derived.java

import org.jspecify.annotations.*;

@NullMarked
public class Derived extends Base {
    @Override
    public BoxDerived<String> foo() { return null; }
}

// FILE: main.kt

fun bar(d: Derived) {
    d.foo().get()<!UNNECESSARY_SAFE_CALL!>?.<!>length
    d.foo().set(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}