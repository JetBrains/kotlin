public abstract interface ASet /* ASet*/<T>  extends java.util.Collection<T>, kotlin.collections.MutableCollection<T>, kotlin.jvm.internal.markers.KMutableCollection {
}

public final class MyList /* MyList*/ implements java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.collections.List<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String get(int);//  get(int)

  public  MyList();//  .ctor()
}

public abstract class MySet /* MySet*/<T>  implements ASet<T> {
  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  MySet();//  .ctor()
}

public abstract class SmartSet /* SmartSet*/<T>  extends kotlin.collections.AbstractMutableSet<T> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<T> iterator();//  iterator()

  @java.lang.Override()
  public boolean add(T);//  add(T)

  private  SmartSet();//  .ctor()
}
