public interface PrivateInTrait extends jet.JetObject {
    @org.jetbrains.annotations.NotNull
    java.lang.String getNn();

    @org.jetbrains.annotations.Nullable
    java.lang.String getN();

    @org.jetbrains.annotations.Nullable
    java.lang.String bar(@org.jetbrains.annotations.NotNull @jet.runtime.typeinfo.JetValueParameter(name = "a") java.lang.String a, @org.jetbrains.annotations.Nullable @jet.runtime.typeinfo.JetValueParameter(name = "b", type = "?") java.lang.String b);
}