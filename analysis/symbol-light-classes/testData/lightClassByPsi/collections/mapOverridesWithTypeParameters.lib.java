public abstract class GenericKeyMutableMap /* test.GenericKeyMutableMap*/<K>  implements java.util.Map<K, java.lang.String>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String getOrDefault(java.lang.Object, @org.jetbrains.annotations.NotNull() java.lang.String);//  getOrDefault(java.lang.Object, java.lang.String)

  public  GenericKeyMutableMap();//  .ctor()

  public abstract boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<java.lang.String> getValues();//  getValues()

  public abstract java.util.Set<K> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<K, java.lang.String>> getEntries();//  getEntries()

  public boolean remove(K, @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(K, java.lang.String)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<java.lang.String> values();//  values()

  public final java.util.Set<K> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<K, java.lang.String>> entrySet();//  entrySet()
}

public abstract class GenericMutableMap /* test.GenericMutableMap*/<K, V>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMutableMap {
  public  GenericMutableMap();//  .ctor()

  public V getOrDefault(java.lang.Object, V);//  getOrDefault(java.lang.Object, V)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<V> getValues();//  getValues()

  public abstract java.util.Set<K> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<K, V>> getEntries();//  getEntries()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<V> values();//  values()

  public final java.util.Set<K> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<K, V>> entrySet();//  entrySet()
}

public abstract class GenericValueMap /* test.GenericValueMap*/<V>  implements java.util.Map<java.lang.String, V>, kotlin.jvm.internal.markers.KMappedMarker {
  public  GenericValueMap();//  .ctor()

  public V compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super V, ? extends V>);//  compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super V, ? extends V>)

  public V computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends V>);//  computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends V>)

  public V computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super V, ? extends V>);//  computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super V, ? extends V>)

  public V getOrDefault(@org.jetbrains.annotations.NotNull() java.lang.String, V);//  getOrDefault(java.lang.String, V)

  public V merge(java.lang.String, V, java.util.function.BiFunction<? super V, ? super V, ? extends V>);//  merge(java.lang.String, V, java.util.function.BiFunction<? super V, ? super V, ? extends V>)

  public V put(java.lang.String, V);//  put(java.lang.String, V)

  public V putIfAbsent(java.lang.String, V);//  putIfAbsent(java.lang.String, V)

  public V remove(java.lang.Object);//  remove(java.lang.Object)

  public V replace(java.lang.String, V);//  replace(java.lang.String, V)

  public abstract V get(java.lang.String);//  get(java.lang.String)

  public abstract boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<V> getValues();//  getValues()

  public abstract java.util.Set<java.lang.String> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<java.lang.String, V>> getEntries();//  getEntries()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(java.lang.String, V, V);//  replace(java.lang.String, V, V)

  public final V get(java.lang.Object);//  get(java.lang.Object)

  public final V getOrDefault(java.lang.Object, V);//  getOrDefault(java.lang.Object, V)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<V> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, V>> entrySet();//  entrySet()

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends java.lang.String, ? extends V>);//  putAll(java.util.Map<? extends java.lang.String, ? extends V>)

  public void replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super V, ? extends V>);//  replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super V, ? extends V>)
}

public abstract class GenericValueMutableMap /* test.GenericValueMutableMap*/<V>  implements java.util.Map<java.lang.String, V>, kotlin.jvm.internal.markers.KMutableMap {
  public  GenericValueMutableMap();//  .ctor()

  public V getOrDefault(@org.jetbrains.annotations.NotNull() java.lang.String, V);//  getOrDefault(java.lang.String, V)

  public abstract V get(java.lang.String);//  get(java.lang.String)

  public abstract V remove(java.lang.String);//  remove(java.lang.String)

  public abstract boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<V> getValues();//  getValues()

  public abstract java.util.Set<java.lang.String> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<java.lang.String, V>> getEntries();//  getEntries()

  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String, V);//  remove(java.lang.String, V)

  public final V get(java.lang.Object);//  get(java.lang.Object)

  public final V getOrDefault(java.lang.Object, V);//  getOrDefault(java.lang.Object, V)

  public final V remove(java.lang.Object);//  remove(java.lang.Object)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<V> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, V>> entrySet();//  entrySet()
}
