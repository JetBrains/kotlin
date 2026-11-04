@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface A /* test.A*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] value();//  value()
}

public final class AllDefaults /* test.AllDefaults*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] xs;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] getXs();//  getXs()

  public  AllDefaults();//  .ctor()

  public /* vararg */  AllDefaults(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface B /* test.B*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] names();//  names()

  public abstract int x();//  x()
}

public abstract class Base /* test.Base*/ {
  protected /* vararg */ void h(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  h(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public  Base();//  .ctor()

  public /* vararg */ void g(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() java.lang.Integer @org.jetbrains.annotations.Nullable() ...);//  g(@org.jetbrains.annotations.Nullable() java.lang.Integer @org.jetbrains.annotations.Nullable() [])

  public abstract /* vararg */ void f(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  f(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])
}

public abstract interface Ctx /* test.Ctx*/ {
}

public final class Derived /* test.Derived*/ extends test.Base {
  @java.lang.Override()
  protected /* vararg */ void h(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  h(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  @java.lang.Override()
  public /* vararg */ void f(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  f(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  @java.lang.Override()
  public /* vararg */ void g(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() java.lang.Integer @org.jetbrains.annotations.Nullable() ...);//  g(@org.jetbrains.annotations.Nullable() java.lang.Integer @org.jetbrains.annotations.Nullable() [])

  public  Derived();//  .ctor()
}

public enum E /* test.E*/ {
  A,
  B;

  @org.jetbrains.annotations.NotNull()
  private final int @org.jetbrains.annotations.NotNull() [] xs;

  @org.jetbrains.annotations.NotNull()
  public final int @org.jetbrains.annotations.NotNull() [] getXs();//  getXs()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<@org.jetbrains.annotations.NotNull() test.E> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() test.E @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() test.E valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  private /* vararg */  E(@org.jetbrains.annotations.NotNull() int...);//  .ctor(int[])
}

public abstract interface F /* test.F*/ {
  public abstract /* vararg */ void invoke(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  invoke(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])
}

public final class Field /* test.Field*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] items;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] getItems();//  getItems()

  public /* vararg */  Field(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])
}

public final class Generic /* test.Generic*/<T>  {
  @org.jetbrains.annotations.NotNull()
  private final T @org.jetbrains.annotations.NotNull() [] items;

  @org.jetbrains.annotations.NotNull()
  public final T @org.jetbrains.annotations.NotNull() [] getItems();//  getItems()

  public /* vararg */  Generic(@org.jetbrains.annotations.NotNull() T...);//  .ctor(T[])
}

public abstract interface I /* test.I*/ {
  public abstract /* vararg */ void abstractMember(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  abstractMember(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public default /* vararg */ void withBody(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  withBody(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public static final class DefaultImpls /* test.I.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static /* vararg */ void withBody(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.I, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  withBody(@org.jetbrains.annotations.NotNull() test.I, @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])
  }
}

public final class Members /* test.Members*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] items;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() test.Members.Companion Companion;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] getItems();//  getItems()

  private final /* vararg */ void privateMember(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  privateMember(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public /* vararg */  Members(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public /* vararg */  Members(int, @org.jetbrains.annotations.NotNull() long...);//  .ctor(int, long[])

  public final /* vararg */ void internalMember$light_idea_test_case(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  internalMember$light_idea_test_case(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public final /* vararg */ void member(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  member(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  class Companion ...
}

public static final class Companion /* test.Members.Companion*/ {
  private  Companion();//  .ctor()

  public final /* vararg */ void nonStatic(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  nonStatic(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public final /* vararg */ void static(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  static(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])
}

public final class MutableProperty /* test.MutableProperty*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] items;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] getItems();//  getItems()

  public /* vararg */  MutableProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public final void setItems(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() []);//  setItems(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])
}

public final class O /* test.O*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() test.O INSTANCE;

  private  O();//  .ctor()

  public final /* vararg */ void static(@org.jetbrains.annotations.NotNull() int...);//  static(int[])
}

public final class OverloadedConstructor /* test.OverloadedConstructor*/ {
  public /* vararg */  OverloadedConstructor(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  .ctor(int, @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])
}

public final class PrivateConstructor /* test.PrivateConstructor*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] xs;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] getXs();//  getXs()

  private /* vararg */  PrivateConstructor(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])
}

public final class VarargParametersKt /* test.VarargParametersKt*/ {
  @org.jetbrains.annotations.Nullable()
  public static final @org.jetbrains.annotations.Nullable() java.lang.Object suspending(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [], @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() kotlin.Unit>);//  suspending(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [], @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() kotlin.Unit>)

  private static final /* vararg */ void privateTopLevel(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  privateTopLevel(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ <T extends java.lang.CharSequence> void bounded(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() ...);// <T extends java.lang.CharSequence>  bounded(@org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ <T> void generic(@org.jetbrains.annotations.NotNull() T...);// <T>  generic(T[])

  public static final /* vararg */ <T> void genericLists(T, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.List<? extends T> @org.jetbrains.annotations.NotNull() ...);// <T>  genericLists(T, @org.jetbrains.annotations.NotNull() java.util.List<? extends T> @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ <T> void genericNotNull(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() ...);// <T>  genericNotNull(@org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void arrays(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] @org.jetbrains.annotations.NotNull() ...);//  arrays(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void elementWildcards(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String> @org.jetbrains.annotations.NotNull() ...);//  elementWildcards(@org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String> @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void extension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() int...);//  extension(@org.jetbrains.annotations.NotNull() java.lang.String, int[])

  public static final /* vararg */ void finalElementLists(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String> @org.jetbrains.annotations.NotNull() ...);//  finalElementLists(@org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String> @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void functions(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function1<? super @org.jetbrains.annotations.NotNull() java.lang.CharSequence, ? extends @org.jetbrains.annotations.NotNull() java.lang.CharSequence> @org.jetbrains.annotations.NotNull() ...);//  functions(@org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function1<? super @org.jetbrains.annotations.NotNull() java.lang.CharSequence, ? extends @org.jetbrains.annotations.NotNull() java.lang.CharSequence> @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void internalTopLevel(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  internalTopLevel(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void lists(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() java.lang.CharSequence> @org.jetbrains.annotations.NotNull() ...);//  lists(@org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() java.lang.CharSequence> @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void mutableLists(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.CharSequence> @org.jetbrains.annotations.NotNull() ...);//  mutableLists(@org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.CharSequence> @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void named(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  named(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void nullableElements(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() java.lang.String @org.jetbrains.annotations.Nullable() ...);//  nullableElements(@org.jetbrains.annotations.Nullable() java.lang.String @org.jetbrains.annotations.Nullable() [])

  public static final /* vararg */ void nullablePrimitives(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.Nullable() java.lang.Integer @org.jetbrains.annotations.Nullable() ...);//  nullablePrimitives(@org.jetbrains.annotations.Nullable() java.lang.Integer @org.jetbrains.annotations.Nullable() [])

  public static final /* vararg */ void overloads(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  overloads(int, @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void primitiveArrays(@org.jetbrains.annotations.NotNull() int @org.jetbrains.annotations.NotNull() [] @org.jetbrains.annotations.NotNull() ...);//  primitiveArrays(int @org.jetbrains.annotations.NotNull() [] @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void primitives(@org.jetbrains.annotations.NotNull() int...);//  primitives(int[])

  public static final /* vararg */ void projectedArrays(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object @org.jetbrains.annotations.NotNull() [] @org.jetbrains.annotations.NotNull() ...);//  projectedArrays(@org.jetbrains.annotations.NotNull() java.lang.Object @org.jetbrains.annotations.NotNull() [] @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void starLists(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.List<?> @org.jetbrains.annotations.NotNull() ...);//  starLists(@org.jetbrains.annotations.NotNull() java.util.List<?> @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void suppressedWildcards(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() java.lang.CharSequence> @org.jetbrains.annotations.NotNull() ...);//  suppressedWildcards(@org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() java.lang.CharSequence> @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void suspendFunctions(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function1<? super @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> @org.jetbrains.annotations.NotNull() ...);//  suspendFunctions(@org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function1<? super @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> @org.jetbrains.annotations.NotNull() [])

  public static final /* vararg */ void withContext(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.Ctx, @org.jetbrains.annotations.NotNull() int...);//  withContext(@org.jetbrains.annotations.NotNull() test.Ctx, int[])

  public static final /* vararg */ void withDefault(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() ...);//  withDefault(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [])

  public static final void notLast(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [], int);//  notLast(@org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [], int)
}
