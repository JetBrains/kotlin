import org.jetbrains.annotations.Nullable;

interface A {

    @Nullable
    <T, U> U foo(U u, T t);
}