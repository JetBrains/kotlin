public abstract class CListIterator /* test.CListIterator*/ implements test.IMutableListIterator {
  public  CListIterator();//  .ctor()
}

public abstract class CListIterator2 /* test.CListIterator2*/ implements test.IMutableListIterator {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer next();//  next()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer previous();//  previous()

  public  CListIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableListIterator);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableListIterator)

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()

  public void add(int);//  add(int)

  public void remove();//  remove()

  public void set(int);//  set(int)
}

public class CListIterator3 /* test.CListIterator3*/ implements test.IMutableListIterator {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer next();//  next()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer previous();//  previous()

  public  CListIterator3();//  .ctor()

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()

  public void add(int);//  add(int)

  public void remove();//  remove()

  public void set(int);//  set(int)
}

public abstract interface IMutableListIterator /* test.IMutableListIterator*/ extends java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.collections.MutableListIterator<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMutableListIterator {
}
