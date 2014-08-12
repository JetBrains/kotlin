public interface Generic <N, NN> {
    N a(@jet.runtime.typeinfo.JetValueParameter(name = "n") N n);

    @org.jetbrains.annotations.NotNull
    NN b(@jet.runtime.typeinfo.JetValueParameter(name = "nn") @org.jetbrains.annotations.NotNull NN nn);

    @org.jetbrains.annotations.Nullable
    N a1(@jet.runtime.typeinfo.JetValueParameter(name = "n", type = "?") @org.jetbrains.annotations.Nullable N n);

    @org.jetbrains.annotations.Nullable
    NN b1(@jet.runtime.typeinfo.JetValueParameter(name = "nn", type = "?") @org.jetbrains.annotations.Nullable NN nn);
}
