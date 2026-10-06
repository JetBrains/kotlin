public final class A /* test.A*/ {
  @kotlin.jvm.JvmField()
  public int plain = 2 /* initializer type: int */;

  public int withPropertyAnnotation = 1 /* initializer type: int */;

  public  A();//  .ctor()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.PROPERTY})
public abstract @interface OnProperty /* test.OnProperty*/ {
}
