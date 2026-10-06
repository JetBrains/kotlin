public final class A /* test.A*/ {
  @kotlin.jvm.JvmField()
  public int plain;

  @kotlin.jvm.JvmField()
  public int withPropertyAnnotation;

  public  A();//  .ctor()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.PROPERTY})
public abstract @interface OnProperty /* test.OnProperty*/ {
}
