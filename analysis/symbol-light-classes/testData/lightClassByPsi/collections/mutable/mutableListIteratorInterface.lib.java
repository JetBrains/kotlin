public abstract class CListIterator /* test.CListIterator*/<Elem>  implements test.IMutableListIterator<Elem> {
  public  CListIterator();//  .ctor()
}

public abstract class CListIterator2 /* test.CListIterator2*/<Elem>  implements test.IMutableListIterator<Elem> {
  public  CListIterator2(@org.jetbrains.annotations.NotNull() test.IMutableListIterator<Elem>);//  .ctor(test.IMutableListIterator<Elem>)

  public Elem next();//  next()

  public Elem previous();//  previous()

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()

  public void add(Elem);//  add(Elem)

  public void remove();//  remove()

  public void set(Elem);//  set(Elem)
}

public class CListIterator3 /* test.CListIterator3*/<Elem>  implements test.IMutableListIterator<Elem> {
  public  CListIterator3();//  .ctor()

  public Elem next();//  next()

  public Elem previous();//  previous()

  public boolean hasNext();//  hasNext()

  public boolean hasPrevious();//  hasPrevious()

  public int nextIndex();//  nextIndex()

  public int previousIndex();//  previousIndex()

  public void add(Elem);//  add(Elem)

  public void remove();//  remove()

  public void set(Elem);//  set(Elem)
}

public abstract interface IMutableListIterator /* test.IMutableListIterator*/<Elem>  extends java.util.ListIterator<Elem>, kotlin.jvm.internal.markers.KMutableListIterator {
}
