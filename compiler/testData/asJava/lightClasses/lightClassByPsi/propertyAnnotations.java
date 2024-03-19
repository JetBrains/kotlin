@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Anno /* Anno*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String p() default "";//  p()
}

public final class C /* C*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() C.Companion Companion;

  private static final int x = 1 /* initializer type: int */;

  private static final int y = 2 /* initializer type: int */;

  public  C();//  .ctor()

  public static final int getY();//  getY()

  class Companion ...
}

public static final class Companion /* C.Companion*/ {
  @Anno(p = "x")
  @java.lang.Deprecated()
  public static void getX$annotations();//  getX$annotations()

  @Anno(p = "y")
  @java.lang.Deprecated()
  @kotlin.jvm.JvmStatic()
  public static void getY$annotations();//  getY$annotations()

  private  Companion();//  .ctor()

  public final int getX();//  getX()

  public final int getY();//  getY()
}

public class O /* O*/ {
  private final int protectedProperty = 1 /* initializer type: int */;

  @Anno(p = "private")
  @java.lang.Deprecated()
  private static void getPrivateProperty$annotations();//  getPrivateProperty$annotations()

  @Anno(p = "protected")
  @java.lang.Deprecated()
  protected static void getProtectedProperty$annotations();//  getProtectedProperty$annotations()

  private final int getPrivateProperty();//  getPrivateProperty()

  protected final int getProtectedProperty();//  getProtectedProperty()

  public  O();//  .ctor()
}

public final class PropertyAnnotationsKt /* PropertyAnnotationsKt*/ {
  @kotlin.jvm.Transient()
  @kotlin.jvm.Volatile()
  private static transient volatile int jvmFlags = 0 /* initializer type: int */;

  @org.jetbrains.annotations.NotNull()
  private static final @org.jetbrains.annotations.NotNull() java.lang.String nonNullable = "" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.Nullable()
  private static final @org.jetbrains.annotations.Nullable() java.lang.String nullable = null /* initializer type: null */;

  private static final int deprecated = 0 /* initializer type: int */;

  @Anno(p = "nonNullable")
  @java.lang.Deprecated()
  public static void getNonNullable$annotations();//  getNonNullable$annotations()

  @Anno(p = "nullable")
  @java.lang.Deprecated()
  public static void getNullable$annotations();//  getNullable$annotations()

  @Anno(p = "property")
  @java.lang.Deprecated()
  public static void getExtensionProperty1$annotations(java.lang.Object);//  getExtensionProperty1$annotations(java.lang.Object)

  @Anno(p = "property")
  @java.lang.Deprecated()
  public static void getExtensionProperty2$annotations(@org.jetbrains.annotations.NotNull() java.util.List);//  getExtensionProperty2$annotations(@org.jetbrains.annotations.NotNull() java.util.List)

  @Anno(p = "property")
  @java.lang.Deprecated()
  public static void getExtensionProperty3$annotations(java.util.Map);//  getExtensionProperty3$annotations(java.util.Map)

  @java.lang.Deprecated()
  @kotlin.Deprecated(message = "deprecated")
  public static void getDeprecated$annotations();//  getDeprecated$annotations()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String getNonNullable();//  getNonNullable()

  @org.jetbrains.annotations.Nullable()
  public static final @org.jetbrains.annotations.Nullable() java.lang.String getNullable();//  getNullable()

  public static final <T> int getExtensionProperty1(@Anno(p = "receiver") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T>  getExtensionProperty1(@org.jetbrains.annotations.NotNull() T)

  public static final <T> int getExtensionProperty2(@Anno(p = "receiver") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() T>);// <T>  getExtensionProperty2(@org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() T>)

  public static final <X, Y extends java.util.List<? extends X>, Z extends java.util.Map<X, ? extends @org.jetbrains.annotations.NotNull() Y>> int getExtensionProperty3(@Anno(p = "receiver") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Z);// <X, Y extends java.util.List<? extends X>, Z extends java.util.Map<X, ? extends @org.jetbrains.annotations.NotNull() Y>>  getExtensionProperty3(@org.jetbrains.annotations.NotNull() Z)

  public static final int getDeprecated();//  getDeprecated()

  public static final int getJvmFlags();//  getJvmFlags()

  public static final void setJvmFlags(int);//  setJvmFlags(int)
}
