public abstract class BelowBoundedList /* test.BelowBoundedList*/ extends test.BoundedList<java.lang.String> {
  public  BelowBoundedList();//  .ctor()
}

public abstract class BelowDirectList /* test.BelowDirectList*/ extends test.DirectList<java.lang.String> {
  public  BelowDirectList();//  .ctor()

  public abstract boolean contains(java.lang.String);//  contains(java.lang.String)

  public abstract int indexOf(java.lang.String);//  indexOf(java.lang.String)

  public abstract int lastIndexOf(java.lang.String);//  lastIndexOf(java.lang.String)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)
}

public abstract class BelowDirectMutableList /* test.BelowDirectMutableList*/ extends test.DirectMutableList<java.lang.String> {
  public  BelowDirectMutableList();//  .ctor()

  public abstract boolean contains(java.lang.String);//  contains(java.lang.String)

  public abstract boolean remove(java.lang.String);//  remove(java.lang.String)

  public abstract int indexOf(java.lang.String);//  indexOf(java.lang.String)

  public abstract int lastIndexOf(java.lang.String);//  lastIndexOf(java.lang.String)

  public abstract java.lang.String removeAt(int);//  removeAt(int)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final java.lang.String remove(int);//  remove(int)
}

public abstract class BelowGenericList /* test.BelowGenericList*/ extends test.GenericList<java.lang.String> {
  public  BelowGenericList();//  .ctor()

  public boolean contains(java.lang.String);//  contains(java.lang.String)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public int indexOf(java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(java.lang.String);//  lastIndexOf(java.lang.String)
}

public abstract class BelowStringList /* test.BelowStringList*/ extends test.StringList {
  public  BelowStringList();//  .ctor()
}

public abstract class BelowStringMutableList /* test.BelowStringMutableList*/ extends test.StringMutableList {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String removeAt(int);//  removeAt(int)

  public  BelowStringMutableList();//  .ctor()
}

public abstract class BoundedList /* test.BoundedList*/<E extends java.lang.CharSequence>  extends kotlin.collections.AbstractList<E> {
  public  BoundedList();//  .ctor()

  public boolean contains(java.lang.CharSequence);//  contains(java.lang.CharSequence)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public int indexOf(java.lang.CharSequence);//  indexOf(java.lang.CharSequence)

  public int lastIndexOf(java.lang.CharSequence);//  lastIndexOf(java.lang.CharSequence)
}

public abstract class DirectList /* test.DirectList*/<E>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMappedMarker {
  public  DirectList();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public E remove(int);//  remove(int)

  public E set(int, E);//  set(int, E)

  public abstract int getSize();//  getSize()

  public boolean add(E);//  add(E)

  public boolean addAll(int, java.util.Collection<? extends E>);//  addAll(int, java.util.Collection<? extends E>)

  public boolean addAll(java.util.Collection<? extends E>);//  addAll(java.util.Collection<? extends E>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.List<E> subList(int, int);//  subList(int, int)

  public java.util.ListIterator<E> listIterator();//  listIterator()

  public java.util.ListIterator<E> listIterator(int);//  listIterator(int)

  public void add(int, E);//  add(int, E)

  public void clear();//  clear()

  public void replaceAll(java.util.function.UnaryOperator<E>);//  replaceAll(java.util.function.UnaryOperator<E>)

  public void sort(java.util.Comparator<? super E>);//  sort(java.util.Comparator<? super E>)
}

public abstract class DirectMutableList /* test.DirectMutableList*/<E>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMutableList {
  public  DirectMutableList();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract E removeAt(int);//  removeAt(int)

  public abstract int getSize();//  getSize()

  public final E remove(int);//  remove(int)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class GenericList /* test.GenericList*/<E>  extends kotlin.collections.AbstractList<E> {
  public  GenericList();//  .ctor()
}

public abstract class IntList /* test.IntList*/ extends kotlin.collections.AbstractList<java.lang.Integer> {
  public  IntList();//  .ctor()

  public boolean contains(int);//  contains(int)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)
}

public final class ListObject /* test.ListObject*/ extends kotlin.collections.AbstractList<java.lang.String> {
  @org.jetbrains.annotations.NotNull()
  public static final test.ListObject INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public java.lang.String get(int);//  get(int)

  private  ListObject();//  .ctor()

  public boolean contains(java.lang.String);//  contains(java.lang.String)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public int getSize();//  getSize()

  public int indexOf(java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(java.lang.String);//  lastIndexOf(java.lang.String)
}

public abstract class NullableIntList /* test.NullableIntList*/ extends kotlin.collections.AbstractList<java.lang.Integer> {
  public  NullableIntList();//  .ctor()

  public boolean contains(java.lang.Integer);//  contains(java.lang.Integer)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public int indexOf(java.lang.Integer);//  indexOf(java.lang.Integer)

  public int lastIndexOf(java.lang.Integer);//  lastIndexOf(java.lang.Integer)
}

public abstract class OverridingMutableList /* test.OverridingMutableList*/ extends kotlin.collections.AbstractMutableList<java.lang.String> {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String removeAt(int);//  removeAt(int)

  public  OverridingMutableList();//  .ctor()

  public boolean contains(java.lang.String);//  contains(java.lang.String)

  public boolean remove(java.lang.String);//  remove(java.lang.String)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final java.lang.String remove(int);//  remove(int)

  public int indexOf(java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(java.lang.String);//  lastIndexOf(java.lang.String)
}

public abstract class StringCollection /* test.StringCollection*/ extends kotlin.collections.AbstractCollection<java.lang.String> {
  public  StringCollection();//  .ctor()

  public boolean contains(java.lang.String);//  contains(java.lang.String)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public java.util.Iterator<java.lang.String> iterator();//  iterator()
}

public abstract class StringList /* test.StringList*/ extends kotlin.collections.AbstractList<java.lang.String> {
  public  StringList();//  .ctor()

  public boolean contains(java.lang.String);//  contains(java.lang.String)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public int indexOf(java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(java.lang.String);//  lastIndexOf(java.lang.String)
}

public abstract class StringMutableCollection /* test.StringMutableCollection*/ extends kotlin.collections.AbstractMutableCollection<java.lang.String> {
  public  StringMutableCollection();//  .ctor()

  public boolean contains(java.lang.String);//  contains(java.lang.String)

  public boolean remove(java.lang.String);//  remove(java.lang.String)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)
}

public abstract class StringMutableList /* test.StringMutableList*/ extends kotlin.collections.AbstractMutableList<java.lang.String> {
  public  StringMutableList();//  .ctor()

  public abstract java.lang.String removeAt(int);//  removeAt(int)

  public boolean contains(java.lang.String);//  contains(java.lang.String)

  public boolean remove(java.lang.String);//  remove(java.lang.String)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final java.lang.String remove(int);//  remove(int)

  public int indexOf(java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(java.lang.String);//  lastIndexOf(java.lang.String)
}

public abstract class StringMutableSet /* test.StringMutableSet*/ extends kotlin.collections.AbstractMutableSet<java.lang.String> {
  public  StringMutableSet();//  .ctor()

  public boolean contains(java.lang.String);//  contains(java.lang.String)

  public boolean remove(java.lang.String);//  remove(java.lang.String)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)
}

public abstract class StringSet /* test.StringSet*/ extends kotlin.collections.AbstractSet<java.lang.String> {
  public  StringSet();//  .ctor()

  public boolean contains(java.lang.String);//  contains(java.lang.String)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)
}
