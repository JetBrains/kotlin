public final class Container /* Container*/ {
  @org.jetbrains.annotations.NotNull()
  public static final Container.Companion Companion;

  public  Container();//  .ctor()

  class Base ...

  class Companion ...

  class Delegate ...

  class Derived ...
}

public static abstract class Base /* Container.Base*/ {
  @org.jetbrains.annotations.NotNull()
  private final Container.Delegate<java.lang.String> a$delegate;

  @org.jetbrains.annotations.NotNull()
  private final Container.Delegate<java.lang.String> b$delegate;

  @org.jetbrains.annotations.NotNull()
  private final Container.Delegate<java.lang.String> mutable$delegate;

  @org.jetbrains.annotations.NotNull()
  private final java.lang.String c = "" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String getD();//  getD()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getA();//  getA()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String getB();//  getB()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String getC();//  getC()

  @org.jetbrains.annotations.Nullable()
  public final java.lang.String getMutable();//  getMutable()

  public  Base();//  .ctor()

  public final void setMutable(@org.jetbrains.annotations.Nullable() java.lang.String);//  setMutable(java.lang.String)
}

public static final class Companion /* Container.Companion*/ {
  @org.jetbrains.annotations.NotNull()
  public final <R> Container.Delegate<R> delegate();// <R>  delegate()

  private  Companion();//  .ctor()
}

public static abstract interface Delegate /* Container.Delegate*/<R>  {
  public abstract R getValue(@org.jetbrains.annotations.Nullable() java.lang.Object, @org.jetbrains.annotations.NotNull() kotlin.reflect.KProperty<?>);//  getValue(java.lang.Object, kotlin.reflect.KProperty<?>)

  public abstract void setValue(@org.jetbrains.annotations.Nullable() java.lang.Object, @org.jetbrains.annotations.NotNull() kotlin.reflect.KProperty<?>, R);//  setValue(java.lang.Object, kotlin.reflect.KProperty<?>, R)

  class DefaultImpls ...
}

public static final class Derived /* Container.Derived*/ extends Container.Base {
  @org.jetbrains.annotations.NotNull()
  private final Container.Delegate<java.lang.String> b$delegate;

  @org.jetbrains.annotations.NotNull()
  private final Container.Delegate<java.lang.String> c$delegate;

  @org.jetbrains.annotations.NotNull()
  private final Container.Delegate<java.lang.String> d$delegate;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public java.lang.String getB();//  getB()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public java.lang.String getC();//  getC()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public java.lang.String getD();//  getD()

  public  Derived();//  .ctor()
}
