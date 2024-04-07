public final class Container /* Container*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Container.Companion Companion;

  public  Container();//  .ctor()

  class Base ...

  class Companion ...

  class Delegate ...

  class Derived ...
}

public static abstract class Base /* Container.Base*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Container.Delegate<@org.jetbrains.annotations.NotNull() java.lang.String> a$delegate;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Container.Delegate<@org.jetbrains.annotations.NotNull() java.lang.String> b$delegate;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Container.Delegate<@org.jetbrains.annotations.Nullable() java.lang.String> mutable$delegate;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String c = "" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getB();//  getB()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getC();//  getC()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String getD();//  getD()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getA();//  getA()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() java.lang.String getMutable();//  getMutable()

  public  Base();//  .ctor()

  public final void setMutable(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.String);//  setMutable(@org.jetbrains.annotations.Nullable() java.lang.String)
}

public static final class Companion /* Container.Companion*/ {
  @org.jetbrains.annotations.NotNull()
  public final <R> @org.jetbrains.annotations.NotNull() Container.Delegate<R> delegate();// <R>  delegate()

  private  Companion();//  .ctor()
}

public static abstract interface Delegate /* Container.Delegate*/<R>  {
  public abstract R getValue(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.reflect.KProperty<?>);//  getValue(@org.jetbrains.annotations.Nullable() java.lang.Object, @org.jetbrains.annotations.NotNull() kotlin.reflect.KProperty<?>)

  public abstract void setValue(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.reflect.KProperty<?>, R);//  setValue(@org.jetbrains.annotations.Nullable() java.lang.Object, @org.jetbrains.annotations.NotNull() kotlin.reflect.KProperty<?>, R)

  class DefaultImpls ...
}

public static final class Derived /* Container.Derived*/ extends Container.Base {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Container.Delegate<@org.jetbrains.annotations.NotNull() java.lang.String> b$delegate;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Container.Delegate<@org.jetbrains.annotations.NotNull() java.lang.String> c$delegate;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Container.Delegate<@org.jetbrains.annotations.NotNull() java.lang.String> d$delegate;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getB();//  getB()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getC();//  getC()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getD();//  getD()

  public  Derived();//  .ctor()
}
