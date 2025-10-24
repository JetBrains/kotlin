public abstract class ABMutableMap /* test.ABMutableMap*/ implements java.util.Map<test.A, test.B>, kotlin.collections.MutableMap<test.A, test.B>, kotlin.jvm.internal.markers.KMutableMap {
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

  public  ABMutableMap();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() test.A);//  containsKey(test.A)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() test.B);//  containsValue(test.B)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends test.A, ? extends test.B>);//  putAll(java.util.Map<? extends test.A, ? extends test.B>)
}
