// SCOPE_DUMP: ImplDerived:foo, DerivedImpl:foo
// FILE: Base.java

import org.jetbrains.annotations.NotNull;

public interface Base {
    Base foo(@NotNull String name);
}

// FILE: Derived.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Derived extends Base {
    @Override
    @NotNull Derived foo(@Nullable String name);
}

// FILE: Impl.kt

abstract class Impl : Base {
    override fun foo(name: String): Base {
        return this
    }
}

// FILE: test.kt

abstract <!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class ImplDerived<!> : Impl(), Derived {
    abstract <!NOTHING_TO_OVERRIDE!>override<!> fun foo(name: String?): Derived
}

abstract class DerivedImpl : Derived, Impl() {
    abstract override fun foo(name: String?): Derived
}
