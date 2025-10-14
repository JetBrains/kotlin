public abstract class CListIterator /* test.CListIterator*/<Elem>  implements test.IMutableListIterator<Elem> {
  public  CListIterator();//  .ctor()
}

public abstract class CListIterator2 /* test.CListIterator2*/<Elem>  implements test.IMutableListIterator<Elem> {
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

  @java.lang.Override()
  public void add(Elem);//  add(Elem)

  @java.lang.Override()
  public void remove();//  remove()

  @java.lang.Override()
  public void set(Elem);//  set(Elem)

  public  CListIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableListIterator<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableListIterator<Elem>)
}

public class CListIterator3 /* test.CListIterator3*/<Elem>  implements test.IMutableListIterator<Elem> {
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

  @java.lang.Override()
  public void add(Elem);//  add(Elem)

  @java.lang.Override()
  public void remove();//  remove()

  @java.lang.Override()
  public void set(Elem);//  set(Elem)

  public  CListIterator3();//  .ctor()
}

public abstract interface IMutableListIterator /* test.IMutableListIterator*/<Elem>  extends java.util.ListIterator<Elem>, kotlin.jvm.internal.markers.KMutableListIterator {
}
