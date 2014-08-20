public interface Trait {
    @org.jetbrains.annotations.NotNull
    java.lang.String notNull(@jet.runtime.typeinfo.JetValueParameter(name = "a") @org.jetbrains.annotations.NotNull java.lang.String a);

    @org.jetbrains.annotations.Nullable
    java.lang.String nullable(@jet.runtime.typeinfo.JetValueParameter(name = "a", type = "?") @org.jetbrains.annotations.Nullable java.lang.String a);

    @org.jetbrains.annotations.NotNull
    java.lang.String notNullWithNN();

    @org.jetbrains.annotations.Nullable
    @org.jetbrains.annotations.NotNull
    java.lang.String notNullWithN();

    @org.jetbrains.annotations.Nullable
    java.lang.String nullableWithN();

    @org.jetbrains.annotations.NotNull
    @org.jetbrains.annotations.Nullable
    java.lang.String nullableWithNN();

    @org.jetbrains.annotations.Nullable
    java.lang.String getNullableVal();

    @org.jetbrains.annotations.Nullable
    java.lang.String getNullableVar();

    void setNullableVar(@jet.runtime.typeinfo.JetValueParameter(name = "<set-?>", type = "?") @org.jetbrains.annotations.Nullable java.lang.String p);

    @org.jetbrains.annotations.NotNull
    java.lang.String getNotNullVal();

    @org.jetbrains.annotations.NotNull
    java.lang.String getNotNullVar();

    void setNotNullVar(@jet.runtime.typeinfo.JetValueParameter(name = "<set-?>") @org.jetbrains.annotations.NotNull java.lang.String p);
}
