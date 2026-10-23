public abstract class GenericKeyMutableMap /* test.GenericKeyMutableMap*/<K>  implements java.util.Map<K, @org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMutableMap {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getOrDefault(java.lang.Object, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getOrDefault(java.lang.Object, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract boolean containsValue(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public abstract java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String> getValues();//  getValues()

  @java.lang.Override()
  public abstract java.util.Set<K> getKeys();//  getKeys()

  @java.lang.Override()
  public abstract java.util.Set<java.util.Map.Entry<K, @org.jetbrains.annotations.NotNull() java.lang.String>> getEntries();//  getEntries()

  @java.lang.Override()
  public boolean remove(K, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(K, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object, @org.jetbrains.annotations.NotNull() java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public final java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String> values();//  values()

  @java.lang.Override()
  public final java.util.Set<K> keySet();//  keySet()

  @java.lang.Override()
  public final java.util.Set<java.util.Map.Entry<K, @org.jetbrains.annotations.NotNull() java.lang.String>> entrySet();//  entrySet()

  public  GenericKeyMutableMap();//  .ctor()
}

public abstract class GenericMutableMap /* test.GenericMutableMap*/<K, V>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMutableMap {
  @java.lang.Override()
  public V getOrDefault(java.lang.Object, V);//  getOrDefault(java.lang.Object, V)

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public abstract java.util.Collection<V> getValues();//  getValues()

  @java.lang.Override()
  public abstract java.util.Set<K> getKeys();//  getKeys()

  @java.lang.Override()
  public abstract java.util.Set<java.util.Map.Entry<K, V>> getEntries();//  getEntries()

  @java.lang.Override()
  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public final java.util.Collection<V> values();//  values()

  @java.lang.Override()
  public final java.util.Set<K> keySet();//  keySet()

  @java.lang.Override()
  public final java.util.Set<java.util.Map.Entry<K, V>> entrySet();//  entrySet()

  public  GenericMutableMap();//  .ctor()
}

public abstract class GenericValueMap /* test.GenericValueMap*/<V>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, V>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  public V compute(@org.jetbrains.annotations.NotNull() java.lang.String, java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() java.lang.String, ? super V, ? extends V>);//  compute(@org.jetbrains.annotations.NotNull() java.lang.String, java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() java.lang.String, ? super V, ? extends V>)

  @java.lang.Override()
  public V computeIfAbsent(@org.jetbrains.annotations.NotNull() java.lang.String, java.util.function.Function<? super @org.jetbrains.annotations.NotNull() java.lang.String, ? extends V>);//  computeIfAbsent(@org.jetbrains.annotations.NotNull() java.lang.String, java.util.function.Function<? super @org.jetbrains.annotations.NotNull() java.lang.String, ? extends V>)

  @java.lang.Override()
  public V computeIfPresent(@org.jetbrains.annotations.NotNull() java.lang.String, java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() java.lang.String, ? super V, ? extends V>);//  computeIfPresent(@org.jetbrains.annotations.NotNull() java.lang.String, java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() java.lang.String, ? super V, ? extends V>)

  @java.lang.Override()
  public V getOrDefault(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, V);//  getOrDefault(@org.jetbrains.annotations.NotNull() java.lang.String, V)

  @java.lang.Override()
  public V merge(@org.jetbrains.annotations.NotNull() java.lang.String, V, java.util.function.BiFunction<? super V, ? super V, ? extends V>);//  merge(@org.jetbrains.annotations.NotNull() java.lang.String, V, java.util.function.BiFunction<? super V, ? super V, ? extends V>)

  @java.lang.Override()
  public V put(@org.jetbrains.annotations.NotNull() java.lang.String, V);//  put(@org.jetbrains.annotations.NotNull() java.lang.String, V)

  @java.lang.Override()
  public V putIfAbsent(@org.jetbrains.annotations.NotNull() java.lang.String, V);//  putIfAbsent(@org.jetbrains.annotations.NotNull() java.lang.String, V)

  @java.lang.Override()
  public V remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public V replace(@org.jetbrains.annotations.NotNull() java.lang.String, V);//  replace(@org.jetbrains.annotations.NotNull() java.lang.String, V)

  @java.lang.Override()
  public abstract V get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public abstract java.util.Collection<V> getValues();//  getValues()

  @java.lang.Override()
  public abstract java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> getKeys();//  getKeys()

  @java.lang.Override()
  public abstract java.util.Set<java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, V>> getEntries();//  getEntries()

  @java.lang.Override()
  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  @java.lang.Override()
  public boolean replace(@org.jetbrains.annotations.NotNull() java.lang.String, V, V);//  replace(@org.jetbrains.annotations.NotNull() java.lang.String, V, V)

  @java.lang.Override()
  public final V get(java.lang.Object);//  get(java.lang.Object)

  @java.lang.Override()
  public final V getOrDefault(@org.jetbrains.annotations.NotNull() java.lang.Object, V);//  getOrDefault(java.lang.Object, V)

  @java.lang.Override()
  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public final java.util.Collection<V> values();//  values()

  @java.lang.Override()
  public final java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> keySet();//  keySet()

  @java.lang.Override()
  public final java.util.Set<java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, V>> entrySet();//  entrySet()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends V>);//  putAll(java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends V>)

  @java.lang.Override()
  public void replaceAll(java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() java.lang.String, ? super V, ? extends V>);//  replaceAll(java.util.function.BiFunction<? super @org.jetbrains.annotations.NotNull() java.lang.String, ? super V, ? extends V>)

  public  GenericValueMap();//  .ctor()
}

public abstract class GenericValueMutableMap /* test.GenericValueMutableMap*/<V>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, V>, kotlin.jvm.internal.markers.KMutableMap {
  @java.lang.Override()
  public V getOrDefault(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, V);//  getOrDefault(@org.jetbrains.annotations.NotNull() java.lang.String, V)

  @java.lang.Override()
  public abstract V get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract V remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public abstract java.util.Collection<V> getValues();//  getValues()

  @java.lang.Override()
  public abstract java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> getKeys();//  getKeys()

  @java.lang.Override()
  public abstract java.util.Set<java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, V>> getEntries();//  getEntries()

  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, V);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String, V)

  @java.lang.Override()
  public final V get(java.lang.Object);//  get(java.lang.Object)

  @java.lang.Override()
  public final V getOrDefault(@org.jetbrains.annotations.NotNull() java.lang.Object, V);//  getOrDefault(java.lang.Object, V)

  @java.lang.Override()
  public final V remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(@org.jetbrains.annotations.NotNull() java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public final java.util.Collection<V> values();//  values()

  @java.lang.Override()
  public final java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> keySet();//  keySet()

  @java.lang.Override()
  public final java.util.Set<java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, V>> entrySet();//  entrySet()

  public  GenericValueMutableMap();//  .ctor()
}
