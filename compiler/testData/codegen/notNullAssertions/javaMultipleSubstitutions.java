import org.jetbrains.annotations.NotNull;

class A<T, U> {
    @NotNull
    T foo() { return null; }
}

class B<T> extends A<T, Integer> {
    @Override
    @NotNull
    T foo() { return null; }
}

class C extends B<String> {
    @Override
    @NotNull
    String foo() { return null; }
}
