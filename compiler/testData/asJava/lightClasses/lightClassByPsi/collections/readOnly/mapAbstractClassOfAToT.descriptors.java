public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ATMap /* test.ATMap*/<T>  implements java.util.Map<test.A, T>, kotlin.collections.Map<test.A, T>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ATMap();//  .ctor()
}

public abstract class ATMap2 /* test.ATMap2*/<T>  implements java.util.Map<test.A, T>, kotlin.collections.Map<test.A, T>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<T> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<test.A, T>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public T get();//  get()

  public  ATMap2();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() test.A);//  containsKey(test.A)

  public boolean containsValue(T);//  containsValue(T)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class ATMap3 /* test.ATMap3*/<T>  implements java.util.Map<test.A, T>, kotlin.collections.Map<test.A, T>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<T> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<test.A, T>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public T get(@org.jetbrains.annotations.NotNull() test.A);//  get(test.A)

  public  ATMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() test.A);//  containsKey(test.A)

  public boolean containsValue(T);//  containsValue(T)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}
