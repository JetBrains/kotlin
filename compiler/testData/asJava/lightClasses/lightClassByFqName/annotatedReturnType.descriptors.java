public abstract interface MyInterface /* test.MyInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @test.TypeAnno() java.lang.String getProperty();//  getProperty()

  @test.RegularAnno()
  public abstract @test.TypeAnno() int function(@test.RegularAnno() @test.TypeAnno() int);//  function(@test.TypeAnno() int)
}
