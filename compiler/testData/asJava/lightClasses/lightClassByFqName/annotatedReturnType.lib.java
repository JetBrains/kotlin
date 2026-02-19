public abstract interface MyInterface /* test.MyInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String getProperty();//  getProperty()

  @test.RegularAnno()
  public abstract int function(@test.RegularAnno() int);//  function(int)

  public static final class DefaultImpls /* test.MyInterface.DefaultImpls*/ {
  }
}
