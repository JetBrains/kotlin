public abstract class CListIterator /* test.CListIterator*/ implements test.IMutableListIterator {
  public  CListIterator();//  .ctor()
}

public abstract class CListIterator2 /* test.CListIterator2*/ implements test.IMutableListIterator {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String next();//  next()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String previous();//  previous()

  public  CListIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableListIterator);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableListIterator)

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()

  public void add(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  public void remove();//  remove()

  public void set(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  set(@org.jetbrains.annotations.NotNull() java.lang.String)
}

public class CListIterator3 /* test.CListIterator3*/ implements test.IMutableListIterator {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String next();//  next()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String previous();//  previous()

  public  CListIterator3();//  .ctor()

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()

  public void add(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  public void remove();//  remove()

  public void set(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  set(@org.jetbrains.annotations.NotNull() java.lang.String)
}

public abstract interface IMutableListIterator /* test.IMutableListIterator*/ extends java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.collections.MutableListIterator<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMutableListIterator {
}
