public final class AnnotatedParameterInInnerClassConstructor {
    public AnnotatedParameterInInnerClassConstructor() { /* compiled code */ }

    public final class Inner {
        public Inner(@test.Anno(x = "a") @org.jetbrains.annotations.NotNull java.lang.String a, @test.Anno(x = "b") @org.jetbrains.annotations.NotNull java.lang.String b) { /* compiled code */ }
    }

    public final class InnerGeneric <T> {
        public InnerGeneric(@test.Anno(x = "a") T a, @test.Anno(x = "b") @org.jetbrains.annotations.NotNull java.lang.String b) { /* compiled code */ }
    }
}