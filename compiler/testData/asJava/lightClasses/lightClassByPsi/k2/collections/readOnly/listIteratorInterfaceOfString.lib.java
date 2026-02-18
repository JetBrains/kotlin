public abstract class CListIterator /* test.CListIterator*/ implements test.IListIterator {
  public  CListIterator();//  .ctor()

  public boolean hasNext();//  hasNext()

  public java.lang.String next();//  next()

  public void add(java.lang.String);//  add(java.lang.String)

  public void remove();//  remove()

  public void set(java.lang.String);//  set(java.lang.String)
}

public abstract class CListIterator2 /* test.CListIterator2*/ implements test.IListIterator {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String next();//  next()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String previous();//  previous()

  public  CListIterator2(@org.jetbrains.annotations.NotNull() test.IListIterator);//  .ctor(test.IListIterator)

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()

  public void add(java.lang.String);//  add(java.lang.String)

  public void remove();//  remove()

  public void set(java.lang.String);//  set(java.lang.String)
}

public class CListIterator3 /* test.CListIterator3*/ implements test.IListIterator {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String next();//  next()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String previous();//  previous()

  public  CListIterator3();//  .ctor()

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()

  public void add(java.lang.String);//  add(java.lang.String)

  public void remove();//  remove()

  public void set(java.lang.String);//  set(java.lang.String)
}

public abstract interface IListIterator /* test.IListIterator*/ extends java.util.ListIterator<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
