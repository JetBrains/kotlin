public enum AnnotatedParameterInEnumConstructor /* test.AnnotatedParameterInEnumConstructor*/ {
  A;

  @org.jetbrains.annotations.NotNull()
  public static test.AnnotatedParameterInEnumConstructor valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static test.AnnotatedParameterInEnumConstructor[] values();//  values()

  private  AnnotatedParameterInEnumConstructor(@test.Anno(x = "a") java.lang.String, @test.Anno(x = "b") java.lang.String);//  .ctor(java.lang.String, java.lang.String)

}