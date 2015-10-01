// FILE: A.java

import org.jetbrains.annotations.*;

public interface A<T> {
    void foo(@NotNull T x, @Nullable T y);
}

// FILE: B1.java

// contains fake_override fun foo(/*0*/ org.jetbrains.annotations.NotNull() x: kotlin.String, /*1*/ org.jetbrains.annotations.Nullable() y: kotlin.String?)
public interface B1 extends A<String> {}

// FILE: B2.java
import org.jetbrains.annotations.*;

public interface B2 extends A<String> {
    // Ok, consistent override
    // override fun foo(/*0*/ org.jetbrains.annotations.NotNull() x: kotlin.String, /*1*/ org.jetbrains.annotations.Nullable() y: kotlin.String?)
    void foo(@NotNull String x, @Nullable String y);
}

// FILE: B3.java
import org.jetbrains.annotations.*;

public interface B3 extends A<String> {
    // inconsistent override, second parameter type is platform
    // TODO: first one should be platform too, but it's not because when substituting T -> String!,
    // value parameter type becomes platform and it can be overridden with @NotNull String.
    // Conceptual problem: we can use type parameter as T?, but there is no way to specify same semantics as in Java `@NotNull T`.

    // override /*1*/ fun foo(/*0*/ org.jetbrains.annotations.Nullable() x: kotlin.String?, /*1*/ org.jetbrains.annotations.NotNull() y: kotlin.String!): kotlin.Unit
    void foo(@Nullable String x, @NotNull String y);
}

// FILE: main.kt

// fake_override fun foo(/*0*/ org.jetbrains.annotations.NotNull() x: kotlin.String, /*1*/ org.jetbrains.annotations.Nullable() y: kotlin.String?): kotlin.Unit
interface C1 : A<String>

// fake_override fun foo(/*0*/ org.jetbrains.annotations.NotNull() x: kotlin.String?, /*1*/ org.jetbrains.annotations.Nullable() y: kotlin.String?): kotlin.Unit
interface C2 : A<String?>

interface C3 : B1 {
    // inconsistent override
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(x: String?, y: String?);
}
