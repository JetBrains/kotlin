public interface Generic <N, NN>  extends jet.JetObject {
    N a(@jet.runtime.typeinfo.JetValueParameter(name = "n") N n);

    @org.jetbrains.annotations.NotNull
    NN b(@org.jetbrains.annotations.NotNull @jet.runtime.typeinfo.JetValueParameter(name = "nn") NN nn);
}