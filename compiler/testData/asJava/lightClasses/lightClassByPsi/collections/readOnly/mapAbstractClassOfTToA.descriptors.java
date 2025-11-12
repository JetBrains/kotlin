public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class TAMap /* test.TAMap*/<T>  implements java.util.Map<T, test.A>, kotlin.collections.Map<T, test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  public  TAMap();//  .ctor()
}

public abstract class TAMap2 /* test.TAMap2*/<T>  implements java.util.Map<T, test.A>, kotlin.collections.Map<T, test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<test.A> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<T> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<T, test.A>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public test.A get();//  get()

  public  TAMap2();//  .ctor()

  public boolean containsKey(T);//  containsKey(T)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() test.A);//  containsValue(test.A)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class TAMap3 /* test.TAMap3*/<T>  implements java.util.Map<T, test.A>, kotlin.collections.Map<T, test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<test.A> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<T> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<T, test.A>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public test.A get(java.lang.Object);//  get(java.lang.Object)

  public  TAMap3();//  .ctor()

  public boolean containsKey(T);//  containsKey(T)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() test.A);//  containsValue(test.A)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}
