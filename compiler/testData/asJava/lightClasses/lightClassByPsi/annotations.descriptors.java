@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* Ann*/ {
  public abstract java.lang.Class<? extends java.lang.Object> arg2();//  arg2()

  public abstract java.lang.Class<?> arg1();//  arg1()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Anno /* Anno*/ {
  public abstract Anno[] x() default {@Anno(p = "a"), @Anno(p = "b")};//  x()

  public abstract java.lang.String p() default "";//  p()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.CLASS, kotlin.annotation.AnnotationTarget.FUNCTION, kotlin.annotation.AnnotationTarget.VALUE_PARAMETER, kotlin.annotation.AnnotationTarget.EXPRESSION})
public abstract @interface Anno2 /* Anno2*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.CLASS, kotlin.annotation.AnnotationTarget.FUNCTION, kotlin.annotation.AnnotationTarget.VALUE_PARAMETER, kotlin.annotation.AnnotationTarget.EXPRESSION})
public abstract @interface Anno3 /* Anno3*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.CLASS, kotlin.annotation.AnnotationTarget.FUNCTION, kotlin.annotation.AnnotationTarget.VALUE_PARAMETER, kotlin.annotation.AnnotationTarget.EXPRESSION})
public abstract @interface Anno4 /* Anno4*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.CLASS, kotlin.annotation.AnnotationTarget.FUNCTION, kotlin.annotation.AnnotationTarget.VALUE_PARAMETER, kotlin.annotation.AnnotationTarget.EXPRESSION})
public abstract @interface Anno5 /* Anno5*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.CLASS, kotlin.annotation.AnnotationTarget.FUNCTION, kotlin.annotation.AnnotationTarget.VALUE_PARAMETER, kotlin.annotation.AnnotationTarget.EXPRESSION})
public abstract @interface Anno6 /* Anno6*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface AnnoWithCompanion /* AnnoWithCompanion*/ {
  @kotlin.jvm.JvmField()
  public static final int x;

  @org.jetbrains.annotations.NotNull()
  public static final AnnoWithCompanion.Companion Companion;

  class Companion ...
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface AnnotatedAttribute /* AnnotatedAttribute*/ {
  @Anno()
  public abstract java.lang.String x();//  x()
}

public static final class Companion /* AnnoWithCompanion.Companion*/ {
  private  Companion();//  .ctor()

  public final void foo();//  foo()
}

public final class CtorAnnotations /* CtorAnnotations*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String x;

  @org.jetbrains.annotations.NotNull()
  private final java.lang.String y;

  @org.jetbrains.annotations.NotNull()
  private final java.lang.String z;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getX();//  getX()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getY();//  getY()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getZ();//  getZ()

  public  CtorAnnotations(@Anno() @org.jetbrains.annotations.NotNull() java.lang.String, @Anno() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String, java.lang.String, java.lang.String)
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Deprecated /* Deprecated*/ {
  public abstract ReplaceWith replaceWith() default @ReplaceWith(expression = "");//  replaceWith()

  public abstract java.lang.String message();//  message()
}

public final class Example /* Example*/ {
  @Ann()
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String foo;

  @org.jetbrains.annotations.NotNull()
  private final java.lang.String bar;

  @org.jetbrains.annotations.NotNull()
  private final java.lang.String quux;

  @Ann()
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getBar();//  getBar()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getFoo();//  getFoo()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getQuux();//  getQuux()

  public  Example(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String, @Ann() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String, java.lang.String, java.lang.String)
}

@Anno()
public final class F /* F*/ implements java.lang.Runnable {
  @org.jetbrains.annotations.NotNull()
  private java.lang.String prop;

  @Anno(p = "f")
  public final void f(@Anno() @org.jetbrains.annotations.NotNull() java.lang.String);//  f(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getProp();//  getProp()

  public  F();//  .ctor()

  public final void setProp(@org.jetbrains.annotations.NotNull() java.lang.String);//  setProp(java.lang.String)
}

@Deprecated(message = "This anno is deprecated, use === instead", replaceWith = @ReplaceWith(expression = "this === other"))
@java.lang.annotation.Documented()
@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.SOURCE)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER})
@kotlin.annotation.MustBeDocumented()
@kotlin.annotation.Retention(value = kotlin.annotation.AnnotationRetention.SOURCE)
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.CLASS, kotlin.annotation.AnnotationTarget.FUNCTION, kotlin.annotation.AnnotationTarget.VALUE_PARAMETER, kotlin.annotation.AnnotationTarget.EXPRESSION})
public abstract @interface Fancy /* Fancy*/ {
}

public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.Nullable()
  private java.lang.String x;

  @Anno()
  public  Foo(error.NonExistentClass);//  .ctor(error.NonExistentClass)

  @Anno()
  public final void f4(@org.jetbrains.annotations.NotNull() java.lang.String);//  f4(java.lang.String)

  @Anno()
  public final void setX(@org.jetbrains.annotations.Nullable() java.lang.String);//  setX(java.lang.String)

  @org.jetbrains.annotations.Nullable()
  public final java.lang.String getX();//  getX()
}

@Ann(arg1 = kotlin.String.class, arg2 = kotlin.Int.class)
public final class MyClass /* MyClass*/ {
  public  MyClass();//  .ctor()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface ReplaceWith /* ReplaceWith*/ {
  public abstract java.lang.String expression();//  expression()
}
