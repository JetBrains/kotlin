public abstract class CListIterator /* test.CListIterator*/ implements test.IListIterator {
  public  CListIterator();//  .ctor()
}

public abstract class CListIterator2 /* test.CListIterator2*/ implements test.IListIterator {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String next();//  next()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String previous();//  previous()

  public  CListIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IListIterator);//  .ctor(@org.jetbrains.annotations.NotNull() test.IListIterator)

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()
}

public class CListIterator3 /* test.CListIterator3*/ implements test.IListIterator {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String next();//  next()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String previous();//  previous()

  public  CListIterator3();//  .ctor()

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()
}

public abstract interface IListIterator /* test.IListIterator*/ extends java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.collections.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
