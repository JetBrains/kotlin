public final class Delegating /* test.Delegating*/ implements java.util.Collection<test.Id>, kotlin.jvm.internal.markers.KMutableCollection {
  @org.jetbrains.annotations.NotNull()
  private final java.util.Collection<test.Id> delegate;

  @kotlin.IgnorableReturnValue()
  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public boolean add(@org.jetbrains.annotations.NotNull() test.Id);//  add(test.Id)

  @kotlin.IgnorableReturnValue()
  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public boolean remove(@org.jetbrains.annotations.NotNull() test.Id);//  remove(test.Id)

  @kotlin.IgnorableReturnValue()
  public boolean add-kRzuEh0(long);//  add-kRzuEh0(long)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends test.Id>);//  addAll(java.util.Collection<? extends test.Id>)

  @kotlin.IgnorableReturnValue()
  public boolean remove-kRzuEh0(long);//  remove-kRzuEh0(long)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @kotlin.IgnorableReturnValue()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public boolean contains(@org.jetbrains.annotations.NotNull() test.Id);//  contains(test.Id)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<test.Id> iterator();//  iterator()

  public  Delegating(@org.jetbrains.annotations.NotNull() java.util.Collection<test.Id>);//  .ctor(java.util.Collection<test.Id>)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean contains-kRzuEh0(long);//  contains-kRzuEh0(long)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public final class DelegatingWithOverrides /* test.DelegatingWithOverrides*/ implements java.util.Collection<test.Id>, kotlin.jvm.internal.markers.KMutableCollection {
  @org.jetbrains.annotations.NotNull()
  private final java.util.Collection<test.Id> delegate;

  @kotlin.IgnorableReturnValue()
  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public boolean add(@org.jetbrains.annotations.NotNull() test.Id);//  add(test.Id)

  @kotlin.IgnorableReturnValue()
  public boolean add-kRzuEh0(long);//  add-kRzuEh0(long)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends test.Id>);//  addAll(java.util.Collection<? extends test.Id>)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public boolean contains(@org.jetbrains.annotations.NotNull() test.Id);//  contains(test.Id)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public boolean remove(@org.jetbrains.annotations.NotNull() test.Id);//  remove(test.Id)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<test.Id> iterator();//  iterator()

  public  DelegatingWithOverrides(@org.jetbrains.annotations.NotNull() java.util.Collection<test.Id>);//  .ctor(java.util.Collection<test.Id>)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean contains-kRzuEh0(long);//  contains-kRzuEh0(long)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove-kRzuEh0(long);//  remove-kRzuEh0(long)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

@kotlin.jvm.JvmInline()
public final class Id /* test.Id*/ {
  private final long id;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  Id(long);//  .ctor(long)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public final long getId();//  getId()

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(long, java.lang.Object);//  equals-impl(long, java.lang.Object)

  public static final boolean equals-impl0(long, long);//  equals-impl0(long, long)

  public static int hashCode-impl(long);//  hashCode-impl(long)

  public static java.lang.String toString-impl(long);//  toString-impl(long)

  public static long constructor-impl(long);//  constructor-impl(long)
}
