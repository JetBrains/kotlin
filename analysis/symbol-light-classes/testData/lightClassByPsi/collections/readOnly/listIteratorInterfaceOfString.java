public abstract class CListIterator /* test.CListIterator*/ implements test.IListIterator {
  @java.lang.Override()
  public void add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public void remove();//  remove()

  @java.lang.Override()
  public void set(@org.jetbrains.annotations.NotNull() java.lang.String);//  set(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  CListIterator();//  .ctor()
}

public abstract class CListIterator2 /* test.CListIterator2*/ implements test.IListIterator {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String next();//  next()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String previous();//  previous()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  @java.lang.Override()
  public boolean hasPrevious();//  hasPrevious()

  @java.lang.Override()
  public int nextIndex();//  nextIndex()

  @java.lang.Override()
  public int previousIndex();//  previousIndex()

  @java.lang.Override()
  public void add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public void remove();//  remove()

  @java.lang.Override()
  public void set(@org.jetbrains.annotations.NotNull() java.lang.String);//  set(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  CListIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IListIterator);//  .ctor(@org.jetbrains.annotations.NotNull() test.IListIterator)
}

public class CListIterator3 /* test.CListIterator3*/ implements test.IListIterator {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String next();//  next()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String previous();//  previous()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  @java.lang.Override()
  public boolean hasPrevious();//  hasPrevious()

  @java.lang.Override()
  public int nextIndex();//  nextIndex()

  @java.lang.Override()
  public int previousIndex();//  previousIndex()

  @java.lang.Override()
  public void add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public void remove();//  remove()

  @java.lang.Override()
  public void set(@org.jetbrains.annotations.NotNull() java.lang.String);//  set(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  CListIterator3();//  .ctor()
}

public abstract interface IListIterator /* test.IListIterator*/ extends java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
