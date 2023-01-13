public abstract interface ASet /* ASet*/<T>  extends java.util.Collection<T>, kotlin.collections.MutableCollection<T>, kotlin.jvm.internal.markers.KMutableCollection {
}

public final class MyList /* MyList*/ implements java.util.List<java.lang.String>, kotlin.collections.List<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String get(int);//  get(int)

  public  MyList();//  .ctor()
}

public abstract class MySet /* MySet*/<T>  implements ASet<T> {
  public  MySet();//  .ctor()

  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(java.lang.String)
}

public abstract class SmartSet /* SmartSet*/<T>  extends kotlin.collections.AbstractMutableSet<T> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<T> iterator();//  iterator()

  private  SmartSet();//  .ctor()

  public boolean add(T);//  add(T)
}
