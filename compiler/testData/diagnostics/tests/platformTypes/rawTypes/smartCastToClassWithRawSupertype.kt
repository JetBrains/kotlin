// FIR_IDENTICAL
// SKIP_TXT
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: Key.java
public interface Key<E> {}
// FILE: UserDataHolder.java
public interface UserDataHolder {
    <T> T getUserData(@NotNull Key<T> key);
}

// FILE: GenericInterface.java
public abstract class GenericClass<E> extends UserDataHolder {}

// FILE: NonGenericClassWithRawSuperType.java
public abstract class NonGenericClassWithRawSuperType extends GenericClass {}

// FILE: main.kt
fun foo(k: Key<Boolean>, a: NonGenericClassWithRawSuperType, b: GenericClass<*>) {
    b.getUserData<Boolean>(k)

    if (b is NonGenericClassWithRawSuperType) {
        b.getUserData<Boolean>(k)
    }
}
