public interface Generic <N, NN> extends kotlin.internal.KObject {
    N a(@jet.runtime.typeinfo.JetValueParameter(name = "n") N n);

    @org.jetbrains.annotations.NotNull
    NN b(@org.jetbrains.annotations.NotNull @jet.runtime.typeinfo.JetValueParameter(name = "nn") NN nn);

    @org.jetbrains.annotations.Nullable
    N a1(@org.jetbrains.annotations.Nullable @jet.runtime.typeinfo.JetValueParameter(name = "n", type = "?") N n);

    @org.jetbrains.annotations.Nullable
    NN b1(@org.jetbrains.annotations.Nullable @jet.runtime.typeinfo.JetValueParameter(name = "nn", type = "?") NN nn);
}
