public abstract class AbstractGetOrDefaultMap /* test.AbstractGetOrDefaultMap*/<K>  extends kotlin.collections.AbstractMap<K, java.lang.String> {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String getOrDefault(java.lang.Object, @org.jetbrains.annotations.NotNull() java.lang.String);//  getOrDefault(java.lang.Object, java.lang.String)

  public  AbstractGetOrDefaultMap();//  .ctor()

  public boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)
}

public abstract class BelowAbstractGetOrDefaultMap /* test.BelowAbstractGetOrDefaultMap*/ extends test.AbstractGetOrDefaultMap<java.lang.String> {
  public  BelowAbstractGetOrDefaultMap();//  .ctor()

  public abstract java.lang.String getOrDefault(java.lang.String, java.lang.String);//  getOrDefault(java.lang.String, java.lang.String)

  public boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String getOrDefault(java.lang.Object, java.lang.String);//  getOrDefault(java.lang.Object, java.lang.String)

  public java.lang.String get(java.lang.String);//  get(java.lang.String)
}

public abstract class BelowDirectMutableMap /* test.BelowDirectMutableMap*/ extends test.DirectMutableMap<java.lang.String, java.lang.String> {
  public  BelowDirectMutableMap();//  .ctor()

  public abstract boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public abstract boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public abstract java.lang.String get(java.lang.String);//  get(java.lang.String)

  public abstract java.lang.String remove(java.lang.String);//  remove(java.lang.String)

  public boolean remove(java.lang.String, java.lang.String);//  remove(java.lang.String, java.lang.String)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String getOrDefault(java.lang.Object, java.lang.String);//  getOrDefault(java.lang.Object, java.lang.String)

  public final java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.String getOrDefault(java.lang.String, java.lang.String);//  getOrDefault(java.lang.String, java.lang.String)
}

public abstract class BelowDirectStringMutableMap /* test.BelowDirectStringMutableMap*/ extends test.DirectStringMutableMap {
  public  BelowDirectStringMutableMap();//  .ctor()
}

public abstract class BelowGenericValueMap /* test.BelowGenericValueMap*/ extends test.GenericValueMap<java.lang.String> {
  public  BelowGenericValueMap();//  .ctor()

  public boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String getOrDefault(java.lang.Object, java.lang.String);//  getOrDefault(java.lang.Object, java.lang.String)

  public java.lang.String get(java.lang.String);//  get(java.lang.String)

  public java.lang.String getOrDefault(java.lang.String, java.lang.String);//  getOrDefault(java.lang.String, java.lang.String)
}

public abstract class BelowStringMap /* test.BelowStringMap*/ extends test.StringMap {
  public  BelowStringMap();//  .ctor()
}

public abstract class DirectMutableMap /* test.DirectMutableMap*/<K, V>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMutableMap {
  public  DirectMutableMap();//  .ctor()

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<V> getValues();//  getValues()

  public abstract java.util.Set<K> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<K, V>> getEntries();//  getEntries()

  public final int size();//  size()

  public final java.util.Collection<V> values();//  values()

  public final java.util.Set<K> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<K, V>> entrySet();//  entrySet()
}

public abstract class DirectStringMutableMap /* test.DirectStringMutableMap*/ implements java.util.Map<java.lang.String, java.lang.String>, kotlin.jvm.internal.markers.KMutableMap {
  public  DirectStringMutableMap();//  .ctor()

  public abstract boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public abstract boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public abstract int getSize();//  getSize()

  public abstract java.lang.String get(java.lang.String);//  get(java.lang.String)

  public abstract java.lang.String remove(java.lang.String);//  remove(java.lang.String)

  public abstract java.util.Collection<java.lang.String> getValues();//  getValues()

  public abstract java.util.Set<java.lang.String> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public final java.util.Collection<java.lang.String> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> entrySet();//  entrySet()
}

public abstract class GenericKeyMutableMap /* test.GenericKeyMutableMap*/<K>  extends kotlin.collections.AbstractMutableMap<K, java.lang.String> {
  public  GenericKeyMutableMap();//  .ctor()

  public boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public boolean remove(java.lang.Object, java.lang.String);//  remove(java.lang.Object, java.lang.String)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)
}

public abstract class GenericMap /* test.GenericMap*/<K, V>  extends kotlin.collections.AbstractMap<K, V> {
  public  GenericMap();//  .ctor()
}

public abstract class GenericValueMap /* test.GenericValueMap*/<V>  extends kotlin.collections.AbstractMap<java.lang.String, V> {
  public  GenericValueMap();//  .ctor()

  public boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public final V get(java.lang.Object);//  get(java.lang.Object)

  public final V getOrDefault(java.lang.Object, V);//  getOrDefault(java.lang.Object, V)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public java.lang.Object get(java.lang.String);//  get(java.lang.String)

  public java.lang.Object getOrDefault(java.lang.String, java.lang.Object);//  getOrDefault(java.lang.String, java.lang.Object)
}

public abstract class IntMap /* test.IntMap*/ extends kotlin.collections.AbstractMap<java.lang.Integer, java.lang.Integer> {
  public  IntMap();//  .ctor()

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(int);//  containsValue(int)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final java.lang.Integer get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.Integer getOrDefault(java.lang.Object, int);//  getOrDefault(java.lang.Object, int)

  public java.lang.Integer get(int);//  get(int)

  public java.lang.Integer getOrDefault(int, int);//  getOrDefault(int, int)
}

public abstract class IntMutableMap /* test.IntMutableMap*/ extends kotlin.collections.AbstractMutableMap<java.lang.Integer, java.lang.Integer> {
  public  IntMutableMap();//  .ctor()

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(int);//  containsValue(int)

  public boolean remove(java.lang.Integer, java.lang.Integer);//  remove(java.lang.Integer, java.lang.Integer)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public final java.lang.Integer get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.Integer getOrDefault(java.lang.Object, int);//  getOrDefault(java.lang.Object, int)

  public final java.lang.Integer remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.Integer get(int);//  get(int)

  public java.lang.Integer getOrDefault(int, int);//  getOrDefault(int, int)

  public java.lang.Integer remove(int);//  remove(int)
}

public final class MapObject /* test.MapObject*/ extends kotlin.collections.AbstractMap<java.lang.String, java.lang.String> {
  @org.jetbrains.annotations.NotNull()
  public static final test.MapObject INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  private  MapObject();//  .ctor()

  public boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String getOrDefault(java.lang.Object, java.lang.String);//  getOrDefault(java.lang.Object, java.lang.String)

  public java.lang.String get(java.lang.String);//  get(java.lang.String)

  public java.lang.String getOrDefault(java.lang.String, java.lang.String);//  getOrDefault(java.lang.String, java.lang.String)
}

public abstract class OverridingMap /* test.OverridingMap*/ extends kotlin.collections.AbstractMap<java.lang.String, java.lang.String> {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String getOrDefault(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  getOrDefault(java.lang.String, java.lang.String)

  public  OverridingMap();//  .ctor()

  public boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String getOrDefault(java.lang.Object, java.lang.String);//  getOrDefault(java.lang.Object, java.lang.String)

  public java.lang.String get(java.lang.String);//  get(java.lang.String)
}

public abstract class StringMap /* test.StringMap*/ extends kotlin.collections.AbstractMap<java.lang.String, java.lang.String> {
  public  StringMap();//  .ctor()

  public boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String getOrDefault(java.lang.Object, java.lang.String);//  getOrDefault(java.lang.Object, java.lang.String)

  public java.lang.String get(java.lang.String);//  get(java.lang.String)

  public java.lang.String getOrDefault(java.lang.String, java.lang.String);//  getOrDefault(java.lang.String, java.lang.String)
}

public abstract class StringMutableMap /* test.StringMutableMap*/ extends kotlin.collections.AbstractMutableMap<java.lang.String, java.lang.String> {
  public  StringMutableMap();//  .ctor()

  public boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public boolean remove(java.lang.String, java.lang.String);//  remove(java.lang.String, java.lang.String)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String getOrDefault(java.lang.Object, java.lang.String);//  getOrDefault(java.lang.Object, java.lang.String)

  public final java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.String get(java.lang.String);//  get(java.lang.String)

  public java.lang.String getOrDefault(java.lang.String, java.lang.String);//  getOrDefault(java.lang.String, java.lang.String)

  public java.lang.String remove(java.lang.String);//  remove(java.lang.String)
}
