@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface KtAnnotation /* KtAnnotation*/ {
  @org.jetbrains.annotations.NotNull()
  public static final KtAnnotation.Companion Companion;

  public static final int CONSTANT = 2 /* initializer type: int */ /* constant value 2 */;

  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.NotNull()
  public static java.lang.Class<?> staticFun(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.reflect.KClass<?>);//  staticFun(java.lang.String, kotlin.reflect.KClass<?>)

  @org.jetbrains.annotations.NotNull()
  public static java.lang.Class<?> getKClassProperty();//  getKClassProperty()

  @org.jetbrains.annotations.NotNull()
  public static java.lang.String getLateinitVariable();//  getLateinitVariable()

  @org.jetbrains.annotations.NotNull()
  public static java.lang.String getStaticVariable();//  getStaticVariable()

  @org.jetbrains.annotations.NotNull()
  public static java.lang.String getStringProperty();//  getStringProperty()

  public abstract int i();//  i()

  public static int getStaticProperty();//  getStaticProperty()

  public static void setLateinitVariable(@org.jetbrains.annotations.NotNull() java.lang.String);//  setLateinitVariable(java.lang.String)

  public static void setStaticVariable(@org.jetbrains.annotations.NotNull() java.lang.String);//  setStaticVariable(java.lang.String)

  class Companion ...
}

public static final class Companion /* KtAnnotation.Companion*/ {
  @org.jetbrains.annotations.NotNull()
  private static final java.lang.String stringProperty;

  @org.jetbrains.annotations.NotNull()
  private static final kotlin.reflect.KClass<?> kClassProperty;

  private static final int staticProperty;

  public static final int CONSTANT = 2 /* initializer type: int */ /* constant value 2 */;

  public static java.lang.String lateinitVariable;

  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.NotNull()
  public final kotlin.reflect.KClass<?> staticFun(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.reflect.KClass<?>);//  staticFun(java.lang.String, kotlin.reflect.KClass<?>)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getLateinitVariable();//  getLateinitVariable()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getStaticVariable();//  getStaticVariable()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getStringProperty();//  getStringProperty()

  @org.jetbrains.annotations.NotNull()
  public final kotlin.reflect.KClass<?> getKClassProperty();//  getKClassProperty()

  private  Companion();//  .ctor()

  public final int getStaticProperty();//  getStaticProperty()

  public final void companionFun();//  companionFun()

  public final void setLateinitVariable(@org.jetbrains.annotations.NotNull() java.lang.String);//  setLateinitVariable(java.lang.String)

  public final void setStaticVariable(@org.jetbrains.annotations.NotNull() java.lang.String);//  setStaticVariable(java.lang.String)
}
