// TARGET_BACKEND: JVM
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

fun box(): String {
    val x1: ImplDerived = object : ImplDerived() {
        override fun foo(name: String?): Derived = this
    }
    x1.foo("")
    (x1 as Base).foo("")
    (x1 as Derived).foo("")
    (x1 as Impl).foo("")

    val x2: DerivedImpl = object : DerivedImpl() {
        override fun foo(name: String?): Derived = this
    }
    x2.foo("")
    (x2 as Base).foo("")
    (x2 as Derived).foo("")
    (x2 as Impl).foo("")

    return "OK"
}