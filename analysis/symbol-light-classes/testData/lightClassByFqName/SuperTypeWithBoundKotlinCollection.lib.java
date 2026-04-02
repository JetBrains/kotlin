public abstract class KotlinClass /* KotlinClass*/<K extends RegularInterface, V extends RegularInterface>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMappedMarker {
  public  KotlinClass();//  .ctor()

  public V compute(K, java.util.function.BiFunction<? super K, ? super V, ? extends V>);//  compute(K, java.util.function.BiFunction<? super K, ? super V, ? extends V>)

  public V computeIfAbsent(K, java.util.function.Function<? super K, ? extends V>);//  computeIfAbsent(K, java.util.function.Function<? super K, ? extends V>)

  public V computeIfPresent(K, java.util.function.BiFunction<? super K, ? super V, ? extends V>);//  computeIfPresent(K, java.util.function.BiFunction<? super K, ? super V, ? extends V>)

  public V merge(K, V, java.util.function.BiFunction<? super V, ? super V, ? extends V>);//  merge(K, V, java.util.function.BiFunction<? super V, ? super V, ? extends V>)

  public V put(K, V);//  put(K, V)

  public V putIfAbsent(K, V);//  putIfAbsent(K, V)

  public V remove(java.lang.Object);//  remove(java.lang.Object)

  public V replace(K, V);//  replace(K, V)

  public abstract V get(K);//  get(K)

  public abstract boolean containsKey(K);//  containsKey(K)

  public abstract boolean containsValue(V);//  containsValue(V)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<V> getValues();//  getValues()

  public abstract java.util.Set<K> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<K, V>> getEntries();//  getEntries()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(K, V, V);//  replace(K, V, V)

  public final V get(java.lang.Object);//  get(java.lang.Object)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<V> values();//  values()

  public final java.util.Set<K> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<K, V>> entrySet();//  entrySet()

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends K, ? extends V>);//  putAll(java.util.Map<? extends K, ? extends V>)

  public void replaceAll(java.util.function.BiFunction<? super K, ? super V, ? extends V>);//  replaceAll(java.util.function.BiFunction<? super K, ? super V, ? extends V>)
}
