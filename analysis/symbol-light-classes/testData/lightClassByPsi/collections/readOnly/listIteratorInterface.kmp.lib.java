public abstract class CListIterator /* test.CListIterator*/<Elem>  implements test.IListIterator<Elem> {
  public  CListIterator();//  .ctor()
}

public abstract class CListIterator2 /* test.CListIterator2*/<Elem>  implements test.IListIterator<Elem> {
  public  CListIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IListIterator<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IListIterator<Elem>)

  public Elem next();//  next()

  public Elem previous();//  previous()

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()
}

public class CListIterator3 /* test.CListIterator3*/<Elem>  implements test.IListIterator<Elem> {
  public  CListIterator3();//  .ctor()

  public Elem next();//  next()

  public Elem previous();//  previous()

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()
}

public abstract interface IListIterator /* test.IListIterator*/<Elem>  extends java.util.ListIterator<Elem>, kotlin.collections.ListIterator<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
