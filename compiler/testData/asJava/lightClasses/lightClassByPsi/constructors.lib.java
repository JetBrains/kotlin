public abstract class A /* A*/ {
  @org.jetbrains.annotations.Nullable()
  private final java.lang.String x;

  @org.jetbrains.annotations.Nullable()
  public final java.lang.String getX();//  getX()

  private  A(java.lang.String);//  .ctor(java.lang.String)

  class C ...
}

public static final class C /* A.C*/ extends A {
  public  C();//  .ctor()
}

public final class AAA /* AAA*/ {
  public  AAA(@org.jetbrains.annotations.NotNull() int[], @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function0<kotlin.Unit>);//  .ctor(int[], kotlin.jvm.functions.Function0<kotlin.Unit>)
}

public final class B /* B*/ {
}

public final class ClassWithPrivateCtor /* ClassWithPrivateCtor*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.util.Set<java.lang.Integer> property;

  @org.jetbrains.annotations.NotNull()
  public final java.util.Set<java.lang.Integer> getProperty();//  getProperty()

  private  ClassWithPrivateCtor(java.util.Set<java.lang.Integer>);//  .ctor(java.util.Set<java.lang.Integer>)
}

public final class Outer /* Outer*/ {
  public  Outer();//  .ctor()

  class Inner ...

  class Nested ...
}

public final class Inner /* Outer.Inner*/ {
  public  Inner(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, java.lang.String)
}

public static final class Nested /* Outer.Nested*/ {
  public  Nested();//  .ctor()

  public  Nested(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, java.lang.String)
}

public final class TestConstructor /* TestConstructor*/ {
  private  TestConstructor(int);//  .ctor(int)
}
