@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface A /* test.A*/ {
  public abstract java.lang.String[] value();//  value()
}

public final class AllDefaults /* test.AllDefaults*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String[] xs;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String[] getXs();//  getXs()

  public  AllDefaults();//  .ctor()

  public /* vararg */  AllDefaults(@org.jetbrains.annotations.NotNull() java.lang.String...);//  .ctor(java.lang.String[])
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface B /* test.B*/ {
  public abstract int x();//  x()

  public abstract java.lang.String[] names();//  names()
}

public abstract class Base /* test.Base*/ {
  protected /* vararg */ void h(@org.jetbrains.annotations.NotNull() java.lang.String...);//  h(java.lang.String[])

  public  Base();//  .ctor()

  public /* vararg */ void g(@org.jetbrains.annotations.NotNull() java.lang.Integer...);//  g(java.lang.Integer[])

  public abstract /* vararg */ void f(@org.jetbrains.annotations.NotNull() java.lang.String...);//  f(java.lang.String[])
}

public abstract interface Ctx /* test.Ctx*/ {
}

public final class Derived /* test.Derived*/ extends test.Base {
  protected /* vararg */ void h(@org.jetbrains.annotations.NotNull() java.lang.String...);//  h(java.lang.String[])

  public  Derived();//  .ctor()

  public /* vararg */ void f(@org.jetbrains.annotations.NotNull() java.lang.String...);//  f(java.lang.String[])

  public /* vararg */ void g(@org.jetbrains.annotations.NotNull() java.lang.Integer...);//  g(java.lang.Integer[])
}

public enum E /* test.E*/ {
  A,
  B;

  @org.jetbrains.annotations.NotNull()
  private final int[] xs;

  @org.jetbrains.annotations.NotNull()
  public final int[] getXs();//  getXs()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<@org.jetbrains.annotations.NotNull() test.E> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() test.E @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() test.E valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  private /* vararg */  E(int...);//  .ctor(int[])
}

public abstract interface F /* test.F*/ {
  public abstract /* vararg */ void invoke(@org.jetbrains.annotations.NotNull() java.lang.String...);//  invoke(java.lang.String[])
}

public final class Field /* test.Field*/ {
  @kotlin.jvm.JvmField()
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String[] items;

  public /* vararg */  Field(@org.jetbrains.annotations.NotNull() java.lang.String...);//  .ctor(java.lang.String[])
}

public final class Generic /* test.Generic*/<T>  {
  @org.jetbrains.annotations.NotNull()
  private final T[] items;

  @org.jetbrains.annotations.NotNull()
  public final T[] getItems();//  getItems()

  public /* vararg */  Generic(@org.jetbrains.annotations.NotNull() T...);//  .ctor(T[])
}

public abstract interface I /* test.I*/ {
  public abstract /* vararg */ void abstractMember(@org.jetbrains.annotations.NotNull() java.lang.String...);//  abstractMember(java.lang.String[])

  public default /* vararg */ void withBody(@org.jetbrains.annotations.NotNull() java.lang.String...);//  withBody(java.lang.String[])

  public static final class DefaultImpls /* test.I.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static /* vararg */ void withBody(@org.jetbrains.annotations.NotNull() test.I, @org.jetbrains.annotations.NotNull() java.lang.String...);//  withBody(test.I, java.lang.String[])
  }
}

public final class Members /* test.Members*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String[] items;

  @org.jetbrains.annotations.NotNull()
  public static final test.Members.Companion Companion;

  @kotlin.jvm.JvmStatic()
  public static final /* vararg */ void static(@org.jetbrains.annotations.NotNull() java.lang.String...);//  static(java.lang.String[])

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String[] getItems();//  getItems()

  private final /* vararg */ void privateMember(java.lang.String...);//  privateMember(java.lang.String[])

  public /* vararg */  Members(@org.jetbrains.annotations.NotNull() java.lang.String...);//  .ctor(java.lang.String[])

  public /* vararg */  Members(int, @org.jetbrains.annotations.NotNull() long...);//  .ctor(int, long[])

  public final /* vararg */ void internalMember$main(@org.jetbrains.annotations.NotNull() java.lang.String...);//  internalMember$main(java.lang.String[])

  public final /* vararg */ void member(@org.jetbrains.annotations.NotNull() java.lang.String...);//  member(java.lang.String[])

  class Companion ...
}

public static final class Companion /* test.Members.Companion*/ {
  @kotlin.jvm.JvmStatic()
  public final /* vararg */ void static(@org.jetbrains.annotations.NotNull() java.lang.String...);//  static(java.lang.String[])

  private  Companion();//  .ctor()

  public final /* vararg */ void nonStatic(@org.jetbrains.annotations.NotNull() java.lang.String...);//  nonStatic(java.lang.String[])
}

public final class MutableProperty /* test.MutableProperty*/ {
  @org.jetbrains.annotations.NotNull()
  private java.lang.String[] items;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String[] getItems();//  getItems()

  public /* vararg */  MutableProperty(@org.jetbrains.annotations.NotNull() java.lang.String...);//  .ctor(java.lang.String[])

  public final void setItems(@org.jetbrains.annotations.NotNull() java.lang.String[]);//  setItems(java.lang.String[])
}

public final class O /* test.O*/ {
  @org.jetbrains.annotations.NotNull()
  public static final test.O INSTANCE;

  @kotlin.jvm.JvmStatic()
  public static final /* vararg */ void static(@org.jetbrains.annotations.NotNull() int...);//  static(int[])

  private  O();//  .ctor()
}

public final class OverloadedConstructor /* test.OverloadedConstructor*/ {
  @kotlin.jvm.JvmOverloads()
  public /* vararg */  OverloadedConstructor(@org.jetbrains.annotations.NotNull() java.lang.String...);//  .ctor(java.lang.String[])

  @kotlin.jvm.JvmOverloads()
  public /* vararg */  OverloadedConstructor(int, @org.jetbrains.annotations.NotNull() java.lang.String...);//  .ctor(int, java.lang.String[])
}

public final class PrivateConstructor /* test.PrivateConstructor*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String[] xs;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String[] getXs();//  getXs()

  private /* vararg */  PrivateConstructor(java.lang.String...);//  .ctor(java.lang.String[])
}

public final class VarargParametersKt /* test.VarargParametersKt*/ {
  @kotlin.jvm.JvmName(name = "renamed")
  public static final /* vararg */ void renamed(@org.jetbrains.annotations.NotNull() java.lang.String...);//  renamed(java.lang.String[])

  @kotlin.jvm.JvmOverloads()
  public static final /* vararg */ void overloads(int, @org.jetbrains.annotations.NotNull() java.lang.String...);//  overloads(int, java.lang.String[])

  @kotlin.jvm.JvmOverloads()
  public static final void overloads();//  overloads()

  @kotlin.jvm.JvmOverloads()
  public static final void overloads(int);//  overloads(int)

  @org.jetbrains.annotations.Nullable()
  public static final java.lang.Object suspending(@org.jetbrains.annotations.NotNull() java.lang.String[], @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super kotlin.Unit>);//  suspending(java.lang.String[], kotlin.coroutines.Continuation<? super kotlin.Unit>)

  private static final /* vararg */ void privateTopLevel(java.lang.String...);//  privateTopLevel(java.lang.String[])

  public static final /* vararg */ <T extends java.lang.CharSequence> void bounded(@org.jetbrains.annotations.NotNull() T...);// <T extends java.lang.CharSequence>  bounded(T[])

  public static final /* vararg */ <T> void generic(@org.jetbrains.annotations.NotNull() T...);// <T>  generic(T[])

  public static final /* vararg */ <T> void genericLists(T, @org.jetbrains.annotations.NotNull() java.util.List<? extends T>...);// <T>  genericLists(T, java.util.List<? extends T>[])

  public static final /* vararg */ <T> void genericNotNull(@org.jetbrains.annotations.NotNull() T...);// <T>  genericNotNull(T[])

  public static final /* vararg */ void arrays(@org.jetbrains.annotations.NotNull() java.lang.String[]...);//  arrays(java.lang.String[][])

  public static final /* vararg */ void elementWildcards(@org.jetbrains.annotations.NotNull() java.util.List<? extends java.lang.String>...);//  elementWildcards(java.util.List<? extends java.lang.String>[])

  public static final /* vararg */ void extension(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() int...);//  extension(java.lang.String, int[])

  public static final /* vararg */ void finalElementLists(@org.jetbrains.annotations.NotNull() java.util.List<java.lang.String>...);//  finalElementLists(java.util.List<java.lang.String>[])

  public static final /* vararg */ void functions(@org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function1<? super java.lang.CharSequence, ? extends java.lang.CharSequence>...);//  functions(kotlin.jvm.functions.Function1<? super java.lang.CharSequence, ? extends java.lang.CharSequence>[])

  public static final /* vararg */ void internalTopLevel(@org.jetbrains.annotations.NotNull() java.lang.String...);//  internalTopLevel(java.lang.String[])

  public static final /* vararg */ void lists(@org.jetbrains.annotations.NotNull() java.util.List<? extends java.lang.CharSequence>...);//  lists(java.util.List<? extends java.lang.CharSequence>[])

  public static final /* vararg */ void mutableLists(@org.jetbrains.annotations.NotNull() java.util.List<java.lang.CharSequence>...);//  mutableLists(java.util.List<java.lang.CharSequence>[])

  public static final /* vararg */ void nullableElements(@org.jetbrains.annotations.NotNull() java.lang.String...);//  nullableElements(java.lang.String[])

  public static final /* vararg */ void nullablePrimitives(@org.jetbrains.annotations.NotNull() java.lang.Integer...);//  nullablePrimitives(java.lang.Integer[])

  public static final /* vararg */ void primitiveArrays(@org.jetbrains.annotations.NotNull() int[]...);//  primitiveArrays(int[][])

  public static final /* vararg */ void primitives(@org.jetbrains.annotations.NotNull() int...);//  primitives(int[])

  public static final /* vararg */ void projectedArrays(@org.jetbrains.annotations.NotNull() java.lang.Object[]...);//  projectedArrays(java.lang.Object[][])

  public static final /* vararg */ void starLists(@org.jetbrains.annotations.NotNull() java.util.List<?>...);//  starLists(java.util.List<?>[])

  public static final /* vararg */ void suppressedWildcards(@org.jetbrains.annotations.NotNull() java.util.List<java.lang.CharSequence>...);//  suppressedWildcards(java.util.List<java.lang.CharSequence>[])

  public static final /* vararg */ void suspendFunctions(@org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function1<? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object>...);//  suspendFunctions(kotlin.jvm.functions.Function1<? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object>[])

  public static final /* vararg */ void withContext(@org.jetbrains.annotations.NotNull() test.Ctx, @org.jetbrains.annotations.NotNull() int...);//  withContext(test.Ctx, int[])

  public static final /* vararg */ void withDefault(@org.jetbrains.annotations.NotNull() java.lang.String...);//  withDefault(java.lang.String[])

  public static final void notLast(@org.jetbrains.annotations.NotNull() java.lang.String[], int);//  notLast(java.lang.String[], int)
}
