public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ABMapEntry /* test.ABMapEntry*/ implements java.util.Map.Entry<test.A, test.B>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ABMapEntry();//  .ctor()

  public test.B setValue(test.B);//  setValue(test.B)
}

public abstract class ABMapEntry2 /* test.ABMapEntry2*/ implements java.util.Map.Entry<test.A, test.B>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public test.A getKey();//  getKey()

  @org.jetbrains.annotations.NotNull()
  public test.B getValue();//  getValue()

  public  ABMapEntry2();//  .ctor()

  public test.B setValue(test.B);//  setValue(test.B)
}

public final class B /* test.B*/ {
  public  B();//  .ctor()
}
