public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ABMap /* test.ABMap*/ implements java.util.Map<test.A, test.B>, kotlin.collections.Map<test.A, test.B>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ABMap();//  .ctor()
}

public abstract class ABMap2 /* test.ABMap2*/ implements java.util.Map<test.A, test.B>, kotlin.collections.Map<test.A, test.B>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<test.B> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<test.A, test.B>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public test.B get();//  get()

  public  ABMap2();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() test.A);//  containsKey(test.A)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() test.B);//  containsValue(test.B)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class ABMap3 /* test.ABMap3*/ implements java.util.Map<test.A, test.B>, kotlin.collections.Map<test.A, test.B>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<test.B> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<test.A, test.B>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public test.B get(@org.jetbrains.annotations.NotNull() test.A);//  get(test.A)

  public  ABMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() test.A);//  containsKey(test.A)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() test.B);//  containsValue(test.B)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public final class B /* test.B*/ {
  public  B();//  .ctor()
}
