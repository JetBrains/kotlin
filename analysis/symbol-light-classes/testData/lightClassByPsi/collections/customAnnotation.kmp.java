@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* test.Ann*/ {
  public abstract int x();//  x()
}

public abstract class CCollection /* test.CCollection*/<Elem>  implements java.util.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  @test.Ann(x = 1)
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @test.Ann(x = 2)
  public final void foo();//  foo()

  public  CCollection();//  .ctor()
}
