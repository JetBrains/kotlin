public abstract class AnyBoundedList /* test.AnyBoundedList*/<E>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMappedMarker {
  public  AnyBoundedList();//  .ctor()
}

public abstract class BoundedCollection /* test.BoundedCollection*/<E extends java.lang.CharSequence>  implements java.util.Collection<E>, kotlin.jvm.internal.markers.KMappedMarker {
  public  BoundedCollection();//  .ctor()
}

public abstract class BoundedList /* test.BoundedList*/<E extends java.lang.CharSequence>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMappedMarker {
  public  BoundedList();//  .ctor()
}

public abstract class BoundedListOverrides /* test.BoundedListOverrides*/<E extends java.lang.CharSequence>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() E);//  contains(@org.jetbrains.annotations.NotNull() E)

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() E);//  indexOf(@org.jetbrains.annotations.NotNull() E)

  public  BoundedListOverrides();//  .ctor()
}

public abstract class BoundedMapOverrides /* test.BoundedMapOverrides*/<K extends java.lang.CharSequence, V extends java.lang.Number>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() V getOrDefault(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() K, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() V);//  getOrDefault(@org.jetbrains.annotations.NotNull() K, @org.jetbrains.annotations.NotNull() V)

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() V get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() K);//  get(@org.jetbrains.annotations.NotNull() K)

  @java.lang.Override()
  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() V);//  containsValue(@org.jetbrains.annotations.NotNull() V)

  public  BoundedMapOverrides();//  .ctor()
}

public abstract class BoundedMutableListOverrides /* test.BoundedMutableListOverrides*/<E extends java.lang.CharSequence>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMutableList {
  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() E);//  remove(@org.jetbrains.annotations.NotNull() E)

  public  BoundedMutableListOverrides();//  .ctor()
}

public abstract class BoundedMutableMapOverrides /* test.BoundedMutableMapOverrides*/<K extends java.lang.CharSequence, V>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMutableMap {
  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() V remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() K);//  remove(@org.jetbrains.annotations.NotNull() K)

  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() K, V);//  remove(@org.jetbrains.annotations.NotNull() K, V)

  public  BoundedMutableMapOverrides();//  .ctor()
}

public abstract class TransitivelyBoundedList /* test.TransitivelyBoundedList*/<T extends java.lang.CharSequence, E extends T>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMappedMarker {
  public  TransitivelyBoundedList();//  .ctor()
}
