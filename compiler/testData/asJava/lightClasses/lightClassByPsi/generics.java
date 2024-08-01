public abstract class C /* C*/<T>  {
  @org.jetbrains.annotations.NotNull()
  private java.util.List<? extends java.lang.CharSequence> constructorParam;

  @org.jetbrains.annotations.Nullable()
  private java.util.HashSet<java.lang.String> sHashSetProp;

  @org.jetbrains.annotations.Nullable()
  private java.util.List<? extends java.lang.CharSequence> csListProp;

  @org.jetbrains.annotations.Nullable()
  private java.util.List<java.lang.String> sListProp;

  @org.jetbrains.annotations.Nullable()
  private java.util.Set<java.lang.String> sMutableSetProp;

  @org.jetbrains.annotations.Nullable()
  private java.util.Set<java.lang.String> sSetProp;

  @org.jetbrains.annotations.NotNull()
  public abstract java.util.Collection<java.util.Collection<java.lang.CharSequence>> nested(@org.jetbrains.annotations.NotNull() java.util.List<? extends java.util.List<? extends java.lang.CharSequence>>);//  nested(java.util.List<? extends java.util.List<? extends java.lang.CharSequence>>)

  @org.jetbrains.annotations.NotNull()
  public abstract java.util.List<java.lang.CharSequence> listCS(@org.jetbrains.annotations.NotNull() java.util.List<? extends java.lang.CharSequence>);//  listCS(java.util.List<? extends java.lang.CharSequence>)

  @org.jetbrains.annotations.NotNull()
  public abstract java.util.List<java.lang.String> listS(@org.jetbrains.annotations.NotNull() java.util.List<java.lang.String>);//  listS(java.util.List<java.lang.String>)

  @org.jetbrains.annotations.NotNull()
  public abstract java.util.Set<java.lang.CharSequence> mutables(@org.jetbrains.annotations.NotNull() java.util.Collection<? super java.lang.Number>, @org.jetbrains.annotations.NotNull() java.util.List<? extends C<?>>);//  mutables(java.util.Collection<? super java.lang.Number>, java.util.List<? extends C<?>>)

  @org.jetbrains.annotations.NotNull()
  public final java.util.List<java.lang.CharSequence> getConstructorParam();//  getConstructorParam()

  @org.jetbrains.annotations.Nullable()
  public final <T extends java.lang.Comparable<? super T>> T max(@org.jetbrains.annotations.Nullable() java.util.Collection<? extends T>);// <T extends java.lang.Comparable<? super T>>  max(java.util.Collection<? extends T>)

  @org.jetbrains.annotations.Nullable()
  public final java.util.HashSet<java.lang.String> getSHashSetProp();//  getSHashSetProp()

  @org.jetbrains.annotations.Nullable()
  public final java.util.List<java.lang.CharSequence> getCsListProp();//  getCsListProp()

  @org.jetbrains.annotations.Nullable()
  public final java.util.List<java.lang.String> getSListProp();//  getSListProp()

  @org.jetbrains.annotations.Nullable()
  public final java.util.Set<java.lang.String> getSMutableSetProp();//  getSMutableSetProp()

  @org.jetbrains.annotations.Nullable()
  public final java.util.Set<java.lang.String> getSSetProp();//  getSSetProp()

  public  C(@org.jetbrains.annotations.NotNull() java.util.List<? extends java.lang.CharSequence>);//  .ctor(java.util.List<? extends java.lang.CharSequence>)

  public final <Q extends T> Q getW(@org.jetbrains.annotations.NotNull() Q);// <Q extends T>  getW(Q)

  public final <V, U extends V> T foo(V, @org.jetbrains.annotations.NotNull() C<V>, @org.jetbrains.annotations.NotNull() kotlin.sequences.Sequence<? extends V>);// <V, U extends V>  foo(V, C<V>, kotlin.sequences.Sequence<? extends V>)

  public final void setConstructorParam(@org.jetbrains.annotations.NotNull() java.util.List<? extends java.lang.CharSequence>);//  setConstructorParam(java.util.List<? extends java.lang.CharSequence>)

  public final void setCsListProp(@org.jetbrains.annotations.Nullable() java.util.List<? extends java.lang.CharSequence>);//  setCsListProp(java.util.List<? extends java.lang.CharSequence>)

  public final void setSHashSetProp(@org.jetbrains.annotations.Nullable() java.util.HashSet<java.lang.String>);//  setSHashSetProp(java.util.HashSet<java.lang.String>)

  public final void setSListProp(@org.jetbrains.annotations.Nullable() java.util.List<java.lang.String>);//  setSListProp(java.util.List<java.lang.String>)

  public final void setSMutableSetProp(@org.jetbrains.annotations.Nullable() java.util.Set<java.lang.String>);//  setSMutableSetProp(java.util.Set<java.lang.String>)

  public final void setSSetProp(@org.jetbrains.annotations.Nullable() java.util.Set<java.lang.String>);//  setSSetProp(java.util.Set<java.lang.String>)
}

public class K /* K*/<T extends K<? extends T>>  {
  public  K();//  .ctor()
}

public final class Sub /* Sub*/ extends K<K<?>> {
  public  Sub();//  .ctor()
}
