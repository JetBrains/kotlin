public final class C1 /* C1*/ extends ValBase implements I1, I2 {
  public  C1();//  .ctor()
}

public final class C2 /* C2*/ extends ValBase implements I1, I2, I3Val {
  public  C2();//  .ctor()
}

public final class C3 /* C3*/ extends VarBase implements I3Var {
  public  C3();//  .ctor()
}

public final class C4 /* C4*/ extends Mid<java.lang.String> {
  public  C4();//  .ctor()
}

public abstract interface I1 /* I1*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.Object getFoo();//  getFoo()
}

public abstract interface I2 /* I2*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.CharSequence getFoo();//  getFoo()
}

public abstract interface I2Generic /* I2Generic*/<T extends java.lang.CharSequence>  extends I1 {
  @org.jetbrains.annotations.NotNull()
  public abstract T getFoo();//  getFoo()
}

public abstract interface I3Val /* I3Val*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String getFoo();//  getFoo()
}

public abstract interface I3Var /* I3Var*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String getFoo();//  getFoo()

  public abstract void setFoo(@org.jetbrains.annotations.NotNull() java.lang.String);//  setFoo(java.lang.String)
}

public class Mid /* Mid*/<T extends java.lang.CharSequence>  extends ValBaseGeneric<T> implements I2Generic<T> {
  public  Mid(@org.jetbrains.annotations.NotNull() T);//  .ctor(T)
}

public class ValBase /* ValBase*/ {
  @kotlin.jvm.JvmField()
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String foo;

  public  ValBase(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)
}

public class ValBaseGeneric /* ValBaseGeneric*/<T>  {
  @kotlin.jvm.JvmField()
  public final T foo;

  public  ValBaseGeneric(T);//  .ctor(T)
}

public class VarBase /* VarBase*/ {
  @kotlin.jvm.JvmField()
  @org.jetbrains.annotations.NotNull()
  public java.lang.String foo;

  public  VarBase(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)
}
