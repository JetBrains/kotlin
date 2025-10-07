// FIR_IDENTICAL
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
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
    override fun foo(name: String): Base = this
}

// FILE: test.kt
abstract class ImplDerived : Impl(), Derived {
    abstract override fun foo(name: String?): Derived
}

abstract class DerivedImpl : Derived, Impl() {
    abstract override fun foo(name: String?): Derived
}

fun test() {
    val implDerived: ImplDerived = <!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>object : ImplDerived() {
        override fun foo(name: String?): Derived = this
    }<!>
    val derivedImpl: DerivedImpl = <!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>object : DerivedImpl() {
        override fun foo(name: String?): Derived = this
    }<!>
    implDerived.foo("")
    derivedImpl.foo("")
}