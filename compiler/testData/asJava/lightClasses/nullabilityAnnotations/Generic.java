public interface Generic <N, NN> {
    N a(N n);

    @org.jetbrains.annotations.NotNull
    NN b(@org.jetbrains.annotations.NotNull NN nn);

    @org.jetbrains.annotations.Nullable
    N a1(@org.jetbrains.annotations.Nullable N n);

    @org.jetbrains.annotations.Nullable
    NN b1(@org.jetbrains.annotations.Nullable NN nn);

    @kotlin.jvm.internal.KotlinSyntheticClass(version = {0, 27, 0}, abiVersion = 27)
    static final class DefaultImpls {
    }
}