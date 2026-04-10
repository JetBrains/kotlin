public final class MyClass /* MyClass*/ {
  @Everything()
  @Field()
  @ParameterPropertyAndField()
  @PropertyAndField()
  private int prop;

  @Everything()
  @Get()
  @kotlin.Deprecated()
  public final int getProp();//  getProp()

  public  MyClass(@Everything() @Param() @ParameterPropertyAndField() int);//  .ctor(int)

  public final void setProp(@Everything() @Param() @ParameterPropertyAndField() int);//  setProp(int)
}
