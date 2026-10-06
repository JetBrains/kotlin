public final class Derived /* test.Derived*/ extends test.Hello$World {
  @org.jetbrains.annotations.NotNull()
  public final test.Hello$World take(@org.jetbrains.annotations.NotNull() test.Hello$World);//  take(test.Hello$World)

  public  Derived();//  .ctor()
}

public final class GreetingContainer /* test.GreetingContainer*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.util.List<test.Hello$World> list;

  @org.jetbrains.annotations.NotNull()
  private final test.Hello$World.Nested nestedInDollar;

  @org.jetbrains.annotations.NotNull()
  private final test.Hello.World hello;

  @org.jetbrains.annotations.NotNull()
  private final test.Outer.Inner regularNested;

  @org.jetbrains.annotations.NotNull()
  public final java.util.List<test.Hello$World> component4();//  component4()

  @org.jetbrains.annotations.NotNull()
  public final java.util.List<test.Hello$World> getList();//  getList()

  @org.jetbrains.annotations.NotNull()
  public final test.GreetingContainer copy(@org.jetbrains.annotations.NotNull() test.Hello$World, @org.jetbrains.annotations.NotNull() test.Hello$World.Nested, @org.jetbrains.annotations.NotNull() test.Outer.Inner, @org.jetbrains.annotations.NotNull() java.util.List<? extends test.Hello$World>);//  copy(test.Hello$World, test.Hello$World.Nested, test.Outer.Inner, java.util.List<? extends test.Hello$World>)

  @org.jetbrains.annotations.NotNull()
  public final test.Hello$World component1();//  component1()

  @org.jetbrains.annotations.NotNull()
  public final test.Hello$World getHello();//  getHello()

  @org.jetbrains.annotations.NotNull()
  public final test.Hello$World.Nested component2();//  component2()

  @org.jetbrains.annotations.NotNull()
  public final test.Hello$World.Nested getNestedInDollar();//  getNestedInDollar()

  @org.jetbrains.annotations.NotNull()
  public final test.Outer.Inner component3();//  component3()

  @org.jetbrains.annotations.NotNull()
  public final test.Outer.Inner getRegularNested();//  getRegularNested()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public  GreetingContainer(@org.jetbrains.annotations.NotNull() test.Hello$World, @org.jetbrains.annotations.NotNull() test.Hello$World.Nested, @org.jetbrains.annotations.NotNull() test.Outer.Inner, @org.jetbrains.annotations.NotNull() java.util.List<? extends test.Hello$World>);//  .ctor(test.Hello$World, test.Hello$World.Nested, test.Outer.Inner, java.util.List<? extends test.Hello$World>)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()
}

public class Hello$World /* test.Hello$World*/ {
  public  Hello$World();//  .ctor()

  class Nested ...
}

public static final class Nested /* test.Hello$World.Nested*/ {
  public  Nested();//  .ctor()
}

public final class Outer /* test.Outer*/ {
  public  Outer();//  .ctor()

  class Inner ...
}

public static final class Inner /* test.Outer.Inner*/ {
  public  Inner();//  .ctor()
}
