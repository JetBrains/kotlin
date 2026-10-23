public abstract class BelowBoundedList /* test.BelowBoundedList*/ extends test.BoundedList<@org.jetbrains.annotations.NotNull() java.lang.String> {
  public  BelowBoundedList();//  .ctor()
}

public abstract class BelowDirectList /* test.BelowDirectList*/ extends test.DirectList<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  public abstract boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public  BelowDirectList();//  .ctor()
}

public abstract class BelowDirectMutableList /* test.BelowDirectMutableList*/ extends test.DirectMutableList<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String removeAt(int);//  removeAt(int)

  @java.lang.Override()
  public abstract boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final @org.jetbrains.annotations.NotNull() java.lang.String remove(int);//  remove(int)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public  BelowDirectMutableList();//  .ctor()
}

public abstract class BelowGenericList /* test.BelowGenericList*/ extends test.GenericList<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  BelowGenericList();//  .ctor()
}

public abstract class BelowStringList /* test.BelowStringList*/ extends test.StringList {
  public  BelowStringList();//  .ctor()
}

public abstract class BelowStringMutableList /* test.BelowStringMutableList*/ extends test.StringMutableList {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String removeAt(int);//  removeAt(int)

  public  BelowStringMutableList();//  .ctor()
}

public abstract class BoundedList /* test.BoundedList*/<E extends java.lang.CharSequence>  extends kotlin.collections.AbstractList<E> {
  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.CharSequence);//  contains(@org.jetbrains.annotations.NotNull() java.lang.CharSequence)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.CharSequence);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.CharSequence)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.CharSequence);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.CharSequence)

  public  BoundedList();//  .ctor()
}

public abstract class DirectList /* test.DirectList*/<E>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public E remove(int);//  remove(int)

  @java.lang.Override()
  public E set(int, E);//  set(int, E)

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public boolean add(E);//  add(E)

  @java.lang.Override()
  public boolean addAll(int, java.util.Collection<? extends E>);//  addAll(int, java.util.Collection<? extends E>)

  @java.lang.Override()
  public boolean addAll(java.util.Collection<? extends E>);//  addAll(java.util.Collection<? extends E>)

  @java.lang.Override()
  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  @java.lang.Override()
  public void add(int, E);//  add(int, E)

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void replaceAll(java.util.function.UnaryOperator<E>);//  replaceAll(java.util.function.UnaryOperator<E>)

  @java.lang.Override()
  public void sort(java.util.Comparator<? super E>);//  sort(java.util.Comparator<? super E>)

  public  DirectList();//  .ctor()
}

public abstract class DirectMutableList /* test.DirectMutableList*/<E>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMutableList {
  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public abstract E removeAt(int);//  removeAt(int)

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public final E remove(int);//  remove(int)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  public  DirectMutableList();//  .ctor()
}

public abstract class GenericList /* test.GenericList*/<E>  extends kotlin.collections.AbstractList<E> {
  public  GenericList();//  .ctor()
}

public abstract class IntList /* test.IntList*/ extends kotlin.collections.AbstractList<@org.jetbrains.annotations.NotNull() java.lang.Integer> {
  @java.lang.Override()
  public boolean contains(int);//  contains(int)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  @java.lang.Override()
  public int indexOf(int);//  indexOf(int)

  @java.lang.Override()
  public int lastIndexOf(int);//  lastIndexOf(int)

  public  IntList();//  .ctor()
}

public final class ListObject /* test.ListObject*/ extends kotlin.collections.AbstractList<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() test.ListObject INSTANCE;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String get(int);//  get(int)

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  private  ListObject();//  .ctor()
}

public abstract class NullableIntList /* test.NullableIntList*/ extends kotlin.collections.AbstractList<@org.jetbrains.annotations.Nullable() java.lang.Integer> {
  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.Nullable() java.lang.Integer);//  contains(@org.jetbrains.annotations.Nullable() java.lang.Integer)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.Nullable() java.lang.Integer);//  indexOf(@org.jetbrains.annotations.Nullable() java.lang.Integer)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.Nullable() java.lang.Integer);//  lastIndexOf(@org.jetbrains.annotations.Nullable() java.lang.Integer)

  public  NullableIntList();//  .ctor()
}

public abstract class OverridingMutableList /* test.OverridingMutableList*/ extends kotlin.collections.AbstractMutableList<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String removeAt(int);//  removeAt(int)

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final @org.jetbrains.annotations.NotNull() java.lang.String remove(int);//  remove(int)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  OverridingMutableList();//  .ctor()
}

public abstract class StringCollection /* test.StringCollection*/ extends kotlin.collections.AbstractCollection<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public  StringCollection();//  .ctor()
}

public abstract class StringList /* test.StringList*/ extends kotlin.collections.AbstractList<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  StringList();//  .ctor()
}

public abstract class StringMutableCollection /* test.StringMutableCollection*/ extends kotlin.collections.AbstractMutableCollection<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public  StringMutableCollection();//  .ctor()
}

public abstract class StringMutableList /* test.StringMutableList*/ extends kotlin.collections.AbstractMutableList<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String removeAt(int);//  removeAt(int)

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final @org.jetbrains.annotations.NotNull() java.lang.String remove(int);//  remove(int)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  StringMutableList();//  .ctor()
}

public abstract class StringMutableSet /* test.StringMutableSet*/ extends kotlin.collections.AbstractMutableSet<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public  StringMutableSet();//  .ctor()
}

public abstract class StringSet /* test.StringSet*/ extends kotlin.collections.AbstractSet<@org.jetbrains.annotations.NotNull() java.lang.String> {
  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public  StringSet();//  .ctor()
}
