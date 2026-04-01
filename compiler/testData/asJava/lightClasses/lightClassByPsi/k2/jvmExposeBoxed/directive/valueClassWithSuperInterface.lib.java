public abstract interface Interface /* pack.Interface*/ {
  public abstract int getPropertyWithValueClass-wCez43g();//  getPropertyWithValueClass-wCez43g()

  public abstract int getRegularVariable();//  getRegularVariable()

  public abstract void functionWithValueParam-0JCZ7rA(int);//  functionWithValueParam-0JCZ7rA(int)

  public abstract void regularFunction();//  regularFunction()

  public abstract void setRegularVariable(int);//  setRegularVariable(int)

  public static final class DefaultImpls /* pack.Interface.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static void regularFunction(@org.jetbrains.annotations.NotNull() pack.Interface);//  regularFunction(pack.Interface)
  }
}

@kotlin.jvm.JvmInline()
public final class ValueClass /* pack.ValueClass*/ implements pack.Interface {
  private final int int;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public pack.ValueClass getPropertyWithValueClass();//  getPropertyWithValueClass()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  ValueClass(int);//  .ctor(int)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public int getRegularVariable();//  getRegularVariable()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public void functionWithValueParam(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  functionWithValueParam(pack.ValueClass)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public void regularFunction();//  regularFunction()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public void setRegularVariable(int);//  setRegularVariable(int)

  @org.jetbrains.annotations.NotNull()
  public static java.lang.String toString-impl(int);//  toString-impl(int)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public final int getInt();//  getInt()

  public int hashCode();//  hashCode()

  public static boolean equals-impl(int, java.lang.Object);//  equals-impl(int, java.lang.Object)

  public static final boolean equals-impl0(int, int);//  equals-impl0(int, int)

  public static int constructor-impl(int);//  constructor-impl(int)

  public static int getPropertyWithValueClass-wCez43g(int);//  getPropertyWithValueClass-wCez43g(int)

  public static int getRegularVariable-impl(int);//  getRegularVariable-impl(int)

  public static int hashCode-impl(int);//  hashCode-impl(int)

  public static void functionWithValueParam-0JCZ7rA(int, int);//  functionWithValueParam-0JCZ7rA(int, int)

  public static void regularFunction-impl(int);//  regularFunction-impl(int)

  public static void setRegularVariable-impl(int, int);//  setRegularVariable-impl(int, int)
}
