@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface AnotherUniversalAnnotation /* AnotherUniversalAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface FieldAnnotation /* FieldAnnotation*/ {
}

public final class MyClass /* MyClass*/ {
  @AnotherUniversalAnnotation()
  private final int x5;

  @FieldAnnotation()
  private final int x1;

  @UniversalAnnotation()
  private final int x6;

  private final int x2;

  private final int x3;

  private final int x4;

  private final int x7;

  public  MyClass(@AnotherUniversalAnnotation() @ParameterAnnotation() @UniversalAnnotation() int, int, @PropertyOrParameterAnnotation() int, @ParameterOrFieldAnnotation() int, int, @AnotherUniversalAnnotation() int, @UniversalAnnotation() int);//  .ctor(int, int, int, int, int, int, int)

  public final int getX1();//  getX1()

  public final int getX2();//  getX2()

  public final int getX3();//  getX3()

  public final int getX4();//  getX4()

  public final int getX5();//  getX5()

  public final int getX6();//  getX6()

  public final int getX7();//  getX7()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface ParameterAnnotation /* ParameterAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface ParameterOrFieldAnnotation /* ParameterOrFieldAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface PropertyAnnotation /* PropertyAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface PropertyOrFieldAnnotation /* PropertyOrFieldAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface PropertyOrParameterAnnotation /* PropertyOrParameterAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface UniversalAnnotation /* UniversalAnnotation*/ {
}
