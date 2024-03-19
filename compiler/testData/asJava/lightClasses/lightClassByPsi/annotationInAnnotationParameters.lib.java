@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface A /* a.A*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface B /* b.B*/ {
  public abstract a.A param();//  param()
}

@b.B(param = @a.A())
public final class C /* b.C*/ {
  public  C();//  .ctor()
}
