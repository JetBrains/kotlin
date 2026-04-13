public final class AnnotatedParameterInInnerClassConstructor /* test.AnnotatedParameterInInnerClassConstructor*/ {
  public  AnnotatedParameterInInnerClassConstructor();//  .ctor()

  public final class Inner /* test.AnnotatedParameterInInnerClassConstructor.Inner*/ {
    public  Inner(@org.jetbrains.annotations.NotNull() @test.Anno(x = "a") @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @test.Anno(x = "b") @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String)
  }

  public final class InnerGeneric /* test.AnnotatedParameterInInnerClassConstructor.InnerGeneric*/<T>  {
    public  InnerGeneric(@test.Anno(x = "a") T, @org.jetbrains.annotations.NotNull() @test.Anno(x = "b") @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(T, @org.jetbrains.annotations.NotNull() java.lang.String)
  }
}
