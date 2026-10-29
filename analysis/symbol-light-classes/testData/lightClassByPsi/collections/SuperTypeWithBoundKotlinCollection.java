public abstract class KotlinClass /* KotlinClass*/<K extends RegularInterface, V extends RegularInterface>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  public V compute(@org.jetbrains.annotations.NotNull() K, java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() K, ? super V, ? extends V>);//  compute(@org.jetbrains.annotations.NotNull() K, java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() K, ? super V, ? extends V>)

  @java.lang.Override()
  public V computeIfAbsent(@org.jetbrains.annotations.NotNull() K, java.util.function.Function<? super @org.jetbrains.annotations.NotNull() K, ? extends V>);//  computeIfAbsent(@org.jetbrains.annotations.NotNull() K, java.util.function.Function<? super @org.jetbrains.annotations.NotNull() K, ? extends V>)

  @java.lang.Override()
  public V computeIfPresent(@org.jetbrains.annotations.NotNull() K, java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() K, ? super V, ? extends V>);//  computeIfPresent(@org.jetbrains.annotations.NotNull() K, java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() K, ? super V, ? extends V>)

  @java.lang.Override()
  public V merge(@org.jetbrains.annotations.NotNull() K, V, java.util.function.BiFunction<? super V, ? super V, ? extends V>);//  merge(@org.jetbrains.annotations.NotNull() K, V, java.util.function.BiFunction<? super V, ? super V, ? extends V>)

  @java.lang.Override()
  public V put(@org.jetbrains.annotations.NotNull() K, V);//  put(@org.jetbrains.annotations.NotNull() K, V)

  @java.lang.Override()
  public V putIfAbsent(@org.jetbrains.annotations.NotNull() K, V);//  putIfAbsent(@org.jetbrains.annotations.NotNull() K, V)

  @java.lang.Override()
  public V remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public V replace(@org.jetbrains.annotations.NotNull() K, V);//  replace(@org.jetbrains.annotations.NotNull() K, V)

  @java.lang.Override()
  public abstract V get(@org.jetbrains.annotations.NotNull() K);//  get(@org.jetbrains.annotations.NotNull() K)

  @java.lang.Override()
  public abstract boolean containsKey(@org.jetbrains.annotations.NotNull() K);//  containsKey(@org.jetbrains.annotations.NotNull() K)

  @java.lang.Override()
  public abstract boolean containsValue(V);//  containsValue(V)

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public abstract java.util.Collection<V> getValues();//  getValues()

  @java.lang.Override()
  public abstract java.util.Set<@org.jetbrains.annotations.NotNull() K> getKeys();//  getKeys()

  @java.lang.Override()
  public abstract java.util.Set<java.util.Map.Entry<@org.jetbrains.annotations.NotNull() K, V>> getEntries();//  getEntries()

  @java.lang.Override()
  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  @java.lang.Override()
  public boolean replace(@org.jetbrains.annotations.NotNull() K, V, V);//  replace(@org.jetbrains.annotations.NotNull() K, V, V)

  @java.lang.Override()
  public final V get(java.lang.Object);//  get(java.lang.Object)

  @java.lang.Override()
  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  @java.lang.Override()
  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public final java.util.Collection<V> values();//  values()

  @java.lang.Override()
  public final java.util.Set<@org.jetbrains.annotations.NotNull() K> keySet();//  keySet()

  @java.lang.Override()
  public final java.util.Set<java.util.Map.Entry<@org.jetbrains.annotations.NotNull() K, V>> entrySet();//  entrySet()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(java.util.Map<? extends @org.jetbrains.annotations.NotNull() K, ? extends V>);//  putAll(java.util.Map<? extends @org.jetbrains.annotations.NotNull() K, ? extends V>)

  @java.lang.Override()
  public void replaceAll(java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() K, ? super V, ? extends V>);//  replaceAll(java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() K, ? super V, ? extends V>)

  public  KotlinClass();//  .ctor()
}

public abstract interface RegularInterface /* RegularInterface*/ {
}
