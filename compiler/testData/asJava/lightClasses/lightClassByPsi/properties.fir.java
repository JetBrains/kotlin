public abstract interface A /* A*/ {
  protected abstract int getInt1();//  getInt1()

  public abstract int getInt2();//  getInt2()

  public abstract void setInt2(int);//  setInt2(int)
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
  private final MyProperty<Foo> delegatedProp2$delegate;

  @org.jetbrains.annotations.NotNull()
  private final error.NonExistentClass d;

  @org.jetbrains.annotations.NotNull()
  private final error.NonExistentClass delegatedProp$delegate;

  @org.jetbrains.annotations.NotNull()
  private final error.NonExistentClass privateDelegated$delegate;

  @org.jetbrains.annotations.NotNull()
  private final java.lang.Object arrayConst = {1, 2} /* initializer type: null */;

  @org.jetbrains.annotations.NotNull()
  private final kotlin.Lazy<java.lang.String> lazyProp$delegate;

  @org.jetbrains.annotations.NotNull()
  private final kotlin.jvm.functions.Function1<java.lang.Integer, java.lang.Integer> sum;

  @org.jetbrains.annotations.NotNull()
  private java.lang.Object privateVarWithGet;

  @org.jetbrains.annotations.NotNull()
  private java.lang.String name = "x" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.NotNull()
  private java.lang.String noAccessors;

  @org.jetbrains.annotations.NotNull()
  private java.lang.String protectedWithPrivateSet = "" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.NotNull()
  private static final error.NonExistentClass contextBean;

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

  private final error.NonExistentClass intConst = 30 /* initializer type: int */;

  private final int f1 = 2 /* initializer type: int */;

  private final long e = 2L /* initializer type: long */;

  private int counter = 0 /* initializer type: int */;

  private int f2 = 3 /* initializer type: int */;

  private int internalWithPrivateSet = 1 /* initializer type: int */;

  private int privateVarWithPrivateSet;

  private java.lang.String internalVarPrivateSet;

  private static final int f1 = 4 /* initializer type: int */;

  private static final int prop3;

  private static int prop7;

  protected java.lang.String protectedLateinitVar;

  public Unresolved subject;

  @org.jetbrains.annotations.NotNull()
  protected final java.lang.String getProtectedLateinitVar();//  getProtectedLateinitVar()

  @org.jetbrains.annotations.NotNull()
  protected final java.lang.String getProtectedWithPrivateSet();//  getProtectedWithPrivateSet()

  @org.jetbrains.annotations.NotNull()
  public final Foo getB();//  getB()

  @org.jetbrains.annotations.NotNull()
  public final Unresolved getSubject();//  getSubject()

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

  public  Foo(int, @org.jetbrains.annotations.NotNull() Foo, boolean, @org.jetbrains.annotations.NotNull() error.NonExistentClass, long);//  .ctor(int, Foo, boolean, error.NonExistentClass, long)

  public final boolean getC();//  getC()

  public final boolean isEmpty();//  isEmpty()

  public final error.NonExistentClass getIntConst();//  getIntConst()

  public final int getCounter();//  getCounter()

  public final int getDelegatedProp2();//  getDelegatedProp2()

  public final int getF1();//  getF1()

  public final int getIntProp(int);//  getIntProp(int)

  public final int getInternalWithPrivateSet$light_idea_test_case();//  getInternalWithPrivateSet$light_idea_test_case()

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

  public final void setSubject(@org.jetbrains.annotations.NotNull() Unresolved);//  setSubject(Unresolved)

  class Companion ...
}

public final class Foo2 /* Foo2*/ {
  @org.jetbrains.annotations.NotNull()
  public final Foo getFoo();//  getFoo()

  @org.jetbrains.annotations.NotNull()
  public final Foo getFoo2();//  getFoo2()

  @org.jetbrains.annotations.NotNull()
  public final Foo getMeNonNullFoo();//  getMeNonNullFoo()

  public  Foo2();//  .ctor()
}

public final class Modifiers /* Modifiers*/ {
  private final int plainField = 1 /* initializer type: int */;

  public  Modifiers();//  .ctor()

  public final int getPlainField();//  getPlainField()
}

public final class MyProperty /* MyProperty*/<T>  {
  public  MyProperty();//  .ctor()

  public final int getValue(T, @org.jetbrains.annotations.NotNull() kotlin.reflect.KProperty<?>);//  getValue(T, kotlin.reflect.KProperty<?>)

  public final void setValue(T, @org.jetbrains.annotations.NotNull() kotlin.reflect.KProperty<?>, int);//  setValue(T, kotlin.reflect.KProperty<?>, int)
}
