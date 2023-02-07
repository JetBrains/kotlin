public abstract interface A /* A*/ {
  protected abstract int getInt1();//  getInt1()

  public abstract int getInt2();//  getInt2()

  public abstract void setInt2$light_idea_test_case(int);//  setInt2$light_idea_test_case(int)
}

public static final class Companion /* Foo.Companion*/ {
  private  Companion();//  .ctor()

  public final int getF1();//  getF1()

  public final int getProp3();//  getProp3()

  public final int getProp7();//  getProp7()

  public final void setProp7(int);//  setProp7(int)
}

public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.NotNull()
  private final Foo b;

  @org.jetbrains.annotations.NotNull()
  private final MyProperty delegatedProp2$delegate;

  @org.jetbrains.annotations.NotNull()
  private final java.lang.Object arrayConst;

  @org.jetbrains.annotations.NotNull()
  private final kotlin.jvm.functions.Function1<java.lang.Integer, java.lang.Integer> sum;

  @org.jetbrains.annotations.NotNull()
  private java.lang.Object privateVarWithGet;

  @org.jetbrains.annotations.NotNull()
  private java.lang.String name;

  @org.jetbrains.annotations.NotNull()
  private java.lang.String noAccessors;

  @org.jetbrains.annotations.NotNull()
  private java.lang.String protectedWithPrivateSet;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String CONSTANT_WITH_ESCAPES = "A\tB\nC\rD'E\"F\\G$H" /* initializer type: java.lang.String */ /* constant value A	B
  C
D'E"F\G$H */;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String SUBSYSTEM_DEPRECATED = "This subsystem is deprecated" /* initializer type: java.lang.String */ /* constant value This subsystem is deprecated */;

  @org.jetbrains.annotations.NotNull()
  public static final Foo.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private java.lang.Boolean isEmptyMutable;

  @org.jetbrains.annotations.Nullable()
  private java.lang.Boolean islowercase;

  @org.jetbrains.annotations.Nullable()
  private java.lang.Integer getInt;

  @org.jetbrains.annotations.Nullable()
  private java.lang.Integer isEmptyInt;

  private boolean c;

  private final error.NonExistentClass d;

  private final error.NonExistentClass delegatedProp$delegate;

  private final error.NonExistentClass lazyProp$delegate;

  private final error.NonExistentClass privateDelegated$delegate;

  private final int f1$1;

  private final java.lang.Void intConst;

  private final long e;

  private int counter;

  private int f2;

  private int internalWithPrivateSet;

  private int privateVarWithPrivateSet;

  private java.lang.String internalVarPrivateSet;

  private static final error.NonExistentClass contextBean;

  private static final int f1;

  private static final int prop3;

  private static int prop7;

  protected java.lang.String protectedLateinitVar;

  public error.NonExistentClass subject;

  @org.jetbrains.annotations.NotNull()
  protected final java.lang.String getProtectedLateinitVar();//  getProtectedLateinitVar()

  @org.jetbrains.annotations.NotNull()
  protected final java.lang.String getProtectedWithPrivateSet();//  getProtectedWithPrivateSet()

  @org.jetbrains.annotations.NotNull()
  public final Foo getB();//  getB()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Object getArrayConst();//  getArrayConst()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getDelegatedProp();//  getDelegatedProp()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getInternalVarPrivateSet$light_idea_test_case();//  getInternalVarPrivateSet$light_idea_test_case()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getLazyProp();//  getLazyProp()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getName();//  getName()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getStringRepresentation$light_idea_test_case();//  getStringRepresentation$light_idea_test_case()

  @org.jetbrains.annotations.NotNull()
  public final kotlin.jvm.functions.Function1<java.lang.Integer, java.lang.Integer> getSum();//  getSum()

  @org.jetbrains.annotations.Nullable()
  public final java.lang.Boolean getIslowercase();//  getIslowercase()

  @org.jetbrains.annotations.Nullable()
  public final java.lang.Boolean isEmptyMutable();//  isEmptyMutable()

  @org.jetbrains.annotations.Nullable()
  public final java.lang.Integer getCounter2();//  getCounter2()

  @org.jetbrains.annotations.Nullable()
  public final java.lang.Integer getGetInt();//  getGetInt()

  @org.jetbrains.annotations.Nullable()
  public final java.lang.Integer isEmptyInt();//  isEmptyInt()

  private final int getPrivateDelegated();//  getPrivateDelegated()

  private final java.lang.String getPrivateValWithGet();//  getPrivateValWithGet()

  private final void setPrivateDelegated(int);//  setPrivateDelegated(int)

  protected final int getF2();//  getF2()

  protected final long getE();//  getE()

  protected final void setF2(int);//  setF2(int)

  protected final void setProtectedLateinitVar(@org.jetbrains.annotations.NotNull() java.lang.String);//  setProtectedLateinitVar(java.lang.String)

  public  Foo(int, @org.jetbrains.annotations.NotNull() Foo, boolean, error.NonExistentClass, long);//  .ctor(int, Foo, boolean, error.NonExistentClass, long)

  public final boolean getC();//  getC()

  public final boolean isEmpty();//  isEmpty()

  public final error.NonExistentClass getSubject();//  getSubject()

  public final int getCounter();//  getCounter()

  public final int getDelegatedProp2();//  getDelegatedProp2()

  public final int getF1();//  getF1()

  public final int getIntProp(int);//  getIntProp(int)

  public final int getInternalWithPrivateSet$light_idea_test_case();//  getInternalWithPrivateSet$light_idea_test_case()

  public final java.lang.Void getIntConst();//  getIntConst()

  public final void setC(boolean);//  setC(boolean)

  public final void setCounter(int);//  setCounter(int)

  public final void setCounter2(@org.jetbrains.annotations.Nullable() java.lang.Integer);//  setCounter2(java.lang.Integer)

  public final void setDelegatedProp(@org.jetbrains.annotations.NotNull() java.lang.String);//  setDelegatedProp(java.lang.String)

  public final void setDelegatedProp2(int);//  setDelegatedProp2(int)

  public final void setEmptyInt(@org.jetbrains.annotations.Nullable() java.lang.Integer);//  setEmptyInt(java.lang.Integer)

  public final void setEmptyMutable(@org.jetbrains.annotations.Nullable() java.lang.Boolean);//  setEmptyMutable(java.lang.Boolean)

  public final void setGetInt(@org.jetbrains.annotations.Nullable() java.lang.Integer);//  setGetInt(java.lang.Integer)

  public final void setIslowercase(@org.jetbrains.annotations.Nullable() java.lang.Boolean);//  setIslowercase(java.lang.Boolean)

  public final void setLazyProp(@org.jetbrains.annotations.NotNull() java.lang.String);//  setLazyProp(java.lang.String)

  public final void setName(@org.jetbrains.annotations.NotNull() java.lang.String);//  setName(java.lang.String)

  public final void setStringRepresentation$light_idea_test_case(@org.jetbrains.annotations.NotNull() java.lang.String);//  setStringRepresentation$light_idea_test_case(java.lang.String)

  public final void setSubject(error.NonExistentClass);//  setSubject(error.NonExistentClass)

  class Companion ...
}

public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.NotNull()
  public final Foo getFoo();//  getFoo()

  @org.jetbrains.annotations.NotNull()
  public final Foo getFoo2();//  getFoo2()

  @org.jetbrains.annotations.NotNull()
  public final Foo getMeNonNullFoo();//  getMeNonNullFoo()

  public  Foo();//  .ctor()
}

public final class Modifiers /* Modifiers*/ {
  private final int plainField;

  public  Modifiers();//  .ctor()

  public final int getPlainField();//  getPlainField()
}

public final class MyProperty /* MyProperty*/<T>  {
  public  MyProperty();//  .ctor()

  public final int getValue(T, @org.jetbrains.annotations.NotNull() kotlin.reflect.KProperty<?>);//  getValue(T, kotlin.reflect.KProperty<?>)

  public final void setValue(T, @org.jetbrains.annotations.NotNull() kotlin.reflect.KProperty<?>, int);//  setValue(T, kotlin.reflect.KProperty<?>, int)
}
