@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* Ann*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Class<? extends @org.jetbrains.annotations.NotNull() java.lang.Object> arg2();//  arg2()

  public abstract @org.jetbrains.annotations.NotNull() java.lang.Class<?> arg1();//  arg1()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Anno /* Anno*/ {
  public abstract @org.jetbrains.annotations.NotNull() Anno @org.jetbrains.annotations.NotNull() [] x() default {Anno(p = "a"), Anno(p = "b")};//  x()

  public abstract @org.jetbrains.annotations.NotNull() java.lang.String p() default "";//  p()
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
  public static final int x = 42 /* initializer type: int */;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() AnnoWithCompanion.Companion Companion;

  class Companion ...
}

public static final class Companion /* AnnoWithCompanion.Companion*/ {
  private  Companion();//  .ctor()

  public final void foo();//  foo()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface AnnotatedAttribute /* AnnotatedAttribute*/ {
  @Anno()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String x();//  x()
}

public final class CtorAnnotations /* CtorAnnotations*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String x;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String y;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String z;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getX();//  getX()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getY();//  getY()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getZ();//  getZ()

  public  CtorAnnotations(@Anno() @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @Anno() @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String)
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Deprecated /* Deprecated*/ {
  public abstract @org.jetbrains.annotations.NotNull() ReplaceWith replaceWith() default @ReplaceWith(expression = "");//  replaceWith()

  public abstract @org.jetbrains.annotations.NotNull() java.lang.String message();//  message()
}

public final class Example /* Example*/ {
  @Ann()
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String foo;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String bar;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String quux;

  @Ann()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getBar();//  getBar()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getFoo();//  getFoo()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getQuux();//  getQuux()

  public  Example(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @Ann() @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String)
}

@Anno()
public final class F /* F*/ implements java.lang.Runnable {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.String prop = "x" /* initializer type: java.lang.String */;

  @Anno(p = "f")
  public final void f(@Anno() @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  f(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getProp();//  getProp()

  public  F();//  .ctor()

  public final void setProp(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setProp(@org.jetbrains.annotations.NotNull() java.lang.String)
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
  private @org.jetbrains.annotations.Nullable() java.lang.String x = null /* initializer type: null */;

  @Anno()
  public  Foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() MyDependency);//  .ctor(@org.jetbrains.annotations.NotNull() MyDependency)

  @Anno()
  public final void f4(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  f4(@org.jetbrains.annotations.NotNull() java.lang.String)

  @Anno()
  public final void setX(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.String);//  setX(@org.jetbrains.annotations.Nullable() java.lang.String)

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() java.lang.String getX();//  getX()
}

@Ann(arg1 = java.lang.String.class, arg2 = int.class)
public final class MyClass /* MyClass*/ {
  public  MyClass();//  .ctor()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface ReplaceWith /* ReplaceWith*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String expression();//  expression()
}
