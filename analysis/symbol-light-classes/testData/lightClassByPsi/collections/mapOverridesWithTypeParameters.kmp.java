public abstract class GenericKeyMutableMap /* test.GenericKeyMutableMap*/<K>  implements java.util.Map<K, @org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMutableMap {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getOrDefault(K, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getOrDefault(K, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean remove(K, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(K, @org.jetbrains.annotations.NotNull() java.lang.String)

  public  GenericKeyMutableMap();//  .ctor()
}

public abstract class GenericMutableMap /* test.GenericMutableMap*/<K, V>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMutableMap {
  @java.lang.Override()
  public V getOrDefault(K, V);//  getOrDefault(K, V)

  @java.lang.Override()
  public boolean remove(K, V);//  remove(K, V)

  public  GenericMutableMap();//  .ctor()
}

public abstract class GenericValueMap /* test.GenericValueMap*/<V>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, V>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  public V getOrDefault(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, V);//  getOrDefault(@org.jetbrains.annotations.NotNull() java.lang.String, V)

  public  GenericValueMap();//  .ctor()
}

public abstract class GenericValueMutableMap /* test.GenericValueMutableMap*/<V>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, V>, kotlin.jvm.internal.markers.KMutableMap {
  @java.lang.Override()
  public V getOrDefault(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, V);//  getOrDefault(@org.jetbrains.annotations.NotNull() java.lang.String, V)

  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, V);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String, V)

  public  GenericValueMutableMap();//  .ctor()
}
