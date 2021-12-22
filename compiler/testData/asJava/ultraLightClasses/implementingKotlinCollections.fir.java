public final class MyList /* MyList*/ implements kotlin.collections.List<java.lang.String> {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String get(int);//  get(int)

  public  MyList();//  .ctor()

}

public abstract interface ASet /* ASet*/<T>  extends kotlin.collections.MutableCollection<T> {
}

public abstract class MySet /* MySet*/<T>  implements ASet<T> {
  public  MySet();//  .ctor()

  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(java.lang.String)

}

public abstract class SmartSet /* SmartSet*/<T>  {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<T> iterator();//  iterator()

  private  SmartSet();//  .ctor()

  public boolean add(T);//  add(T)

}
