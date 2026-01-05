@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface MyClass /* MyClass*/ {
  @Everything()
  @Get()
  public abstract int prop();//  prop()

  public static final class DefaultImpls /* MyClass.DefaultImpls*/ {
  }
}
