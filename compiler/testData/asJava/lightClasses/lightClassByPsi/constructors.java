public abstract class A /* A*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() java.lang.String x = null /* initializer type: null */;

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() java.lang.String getX();//  getX()

  protected  A(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.String);//  .ctor(@org.jetbrains.annotations.Nullable() java.lang.String)

  class C ...
}

public static final class C /* A.C*/ extends A {
  public  C();//  .ctor()
}

public final class AAA /* AAA*/ {
  public /* vararg */  AAA(@org.jetbrains.annotations.NotNull() int[], @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function0<@org.jetbrains.annotations.NotNull() kotlin.Unit>);//  .ctor(int[], @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function0<@org.jetbrains.annotations.NotNull() kotlin.Unit>)
}

public final class B /* B*/ {
}

public final class ClassWithPrivateCtor /* ClassWithPrivateCtor*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.Integer> property;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.Integer> getProperty();//  getProperty()

  private  ClassWithPrivateCtor(@org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  .ctor(@org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.Integer>)
}

public final class Outer /* Outer*/ {
  public  Outer();//  .ctor()

  class Inner ...

  class Nested ...
}

public final class Inner /* Outer.Inner*/ {
  public  Inner(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, @org.jetbrains.annotations.NotNull() java.lang.String)
}

public static final class Nested /* Outer.Nested*/ {
  public  Nested();//  .ctor()

  public  Nested(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, @org.jetbrains.annotations.NotNull() java.lang.String)
}

public final class TestConstructor /* TestConstructor*/ {
  private  TestConstructor(int);//  .ctor(int)
}
