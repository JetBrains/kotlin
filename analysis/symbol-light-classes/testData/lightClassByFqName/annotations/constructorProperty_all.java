public final class MyClass /* MyClass*/ {
  @Everything()
  @Field()
  @ParameterPropertyAndField()
  @PropertyAndField()
  @java.lang.Deprecated()
  private int prop;

  @Everything()
  @Get()
  @java.lang.Deprecated()
  @kotlin.Deprecated(message = "Obsolete")
  public final int getProp();//  getProp()

  @java.lang.Deprecated()
  public final void setProp(@Everything() @Param() @ParameterPropertyAndField() int);//  setProp(int)

  public  MyClass(@Everything() @Param() @ParameterPropertyAndField() int);//  .ctor(int)
}
