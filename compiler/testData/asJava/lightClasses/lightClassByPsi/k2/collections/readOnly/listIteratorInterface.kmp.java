public abstract class CListIterator /* test.CListIterator*/<Elem>  implements test.IListIterator<Elem> {
  public  CListIterator();//  .ctor()
}

public abstract class CListIterator2 /* test.CListIterator2*/<Elem>  implements test.IListIterator<Elem> {
  @java.lang.Override()
  public Elem next();//  next()

  @java.lang.Override()
  public Elem previous();//  previous()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  @java.lang.Override()
  public boolean hasPrevious();//  hasPrevious()

  @java.lang.Override()
  public int nextIndex();//  nextIndex()

  @java.lang.Override()
  public int previousIndex();//  previousIndex()

  public  CListIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IListIterator<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IListIterator<Elem>)
}

public class CListIterator3 /* test.CListIterator3*/<Elem>  implements test.IListIterator<Elem> {
  @java.lang.Override()
  public Elem next();//  next()

  @java.lang.Override()
  public Elem previous();//  previous()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  @java.lang.Override()
  public boolean hasPrevious();//  hasPrevious()

  @java.lang.Override()
  public int nextIndex();//  nextIndex()

  @java.lang.Override()
  public int previousIndex();//  previousIndex()

  public  CListIterator3();//  .ctor()
}

public abstract interface IListIterator /* test.IListIterator*/<Elem>  extends java.util.ListIterator<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
