public final class A /* test.A*/ {
  private int plain = 2 /* initializer type: int */;

  private int withPropertyAnnotation = 1 /* initializer type: int */;

  public  A();//  .ctor()

  public final int getPlain();//  getPlain()

  public final int getWithPropertyAnnotation();//  getWithPropertyAnnotation()

  public final void setPlain(int);//  setPlain(int)

  public final void setWithPropertyAnnotation(int);//  setWithPropertyAnnotation(int)
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.PROPERTY})
public abstract @interface OnProperty /* test.OnProperty*/ {
}
