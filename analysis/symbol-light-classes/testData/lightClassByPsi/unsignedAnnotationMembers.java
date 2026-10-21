@Unsigned(ui = UInt.MAX_VALUE - 1u, ul = ULong.MAX_VALUE - 1u, uis = {UInt.MAX_VALUE - 1u})
public final class Annotated /* Annotated*/ {
  public  Annotated();//  .ctor()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Unsigned /* Unsigned*/ {
  public abstract int i() default 1;//  i()
}
