public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ABMutableMap /* test.ABMutableMap*/ implements java.util.Map<test.A, test.B>, kotlin.collections.MutableMap<test.A, test.B>, kotlin.jvm.internal.markers.KMutableMap {
  public  ABMutableMap();//  .ctor()
}

public abstract class ABMutableMap2 /* test.ABMutableMap2*/ implements java.util.Map<test.A, test.B>, kotlin.collections.MutableMap<test.A, test.B>, kotlin.jvm.internal.markers.KMutableMap {
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public test.B put(@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B);//  put(test.A, test.B)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public test.B remove();//  remove()

  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<test.B> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<test.A, test.B>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public test.B get();//  get()

  public  ABMutableMap2();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() test.A);//  containsKey(test.A)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() test.B);//  containsValue(test.B)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends test.A, ? extends test.B>);//  putAll(java.util.Map<? extends test.A, ? extends test.B>)
}

public class ABMutableMap3 /* test.ABMutableMap3*/ implements java.util.Map<test.A, test.B>, kotlin.collections.MutableMap<test.A, test.B>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<test.B> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<test.A, test.B>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public test.B get(@org.jetbrains.annotations.NotNull() test.A);//  get(test.A)

  @org.jetbrains.annotations.Nullable()
  public test.B put(@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B);//  put(test.A, test.B)

  @org.jetbrains.annotations.Nullable()
  public test.B remove(@org.jetbrains.annotations.NotNull() test.A);//  remove(test.A)

  public  ABMutableMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() test.A);//  containsKey(test.A)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() test.B);//  containsValue(test.B)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends test.A, ? extends test.B>);//  putAll(java.util.Map<? extends test.A, ? extends test.B>)
}

public final class B /* test.B*/ {
  public  B();//  .ctor()
}
