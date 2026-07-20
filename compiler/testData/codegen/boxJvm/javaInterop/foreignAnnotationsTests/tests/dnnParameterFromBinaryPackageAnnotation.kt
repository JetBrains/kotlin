// TARGET_BACKEND: JVM_IR
// JSR305_GLOBAL_REPORT: strict
// WITH_JSR305_TEST_ANNOTATIONS

// Regression test for reading package-level default-nullability annotations off a *compiled*
// (binary) dependency. The `test` package below carries a @NonNullApi (JSR-305
// @TypeQualifierDefault({METHOD, PARAMETER})) default in its `package-info`, and `lib` is a
// binary dependency of `main`, so `main` sees `test.Transformer`/`test.Provider` and the
// package default through the library/deserializer path.
//
// Modelled on Gradle's `Provider<T>.map(Transformer<? extends S, ? super T>)` as used in
// dokka's `GradleSourceLinkBuilder` (`localDirectory.map { it.relativeToOrSelf(...) }` on a
// `Property<File?>`): the @NonNullApi default makes `Transformer.transform`'s parameter
// non-null, so after substituting the explicitly nullable type argument the lambda parameter
// `it` is the definitely-non-null `String` and the member call below is allowed. If the binary
// package default is dropped, `it` is `String?` and `it.substring(0)` is a spurious UNSAFE_CALL
// compile error (the exact symptom reported against user projects when `java-direct` failed to
// read binary `package-info.class` annotations).

// MODULE: lib
// FILE: test/package-info.java
@NonNullApi
package test;

// FILE: test/Transformer.java
package test;

public interface Transformer<OUT, IN> {
    OUT transform(IN input);
}

// FILE: test/Provider.java
package test;

public class Provider<T> {
    private final T value;

    public Provider(T value) {
        this.value = value;
    }

    public <S> S map(Transformer<? extends S, ? super T> transformer) {
        return transformer.transform(value);
    }
}

// MODULE: main(lib)
// FILE: main.kt
import test.Provider

fun box(): String {
    val provider: Provider<String?> = Provider("OK")
    val result: String = provider.map { it.substring(0) }
    return result
}
