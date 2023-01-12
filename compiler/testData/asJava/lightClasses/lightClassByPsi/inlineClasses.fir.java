public enum Foo /* Foo*/ {
  ;

  private final int x;

  public final int getX();//  getX()
}

public final class InlineInheritance /* InlineInheritance*/ implements I {
  private final int v;

  @java.lang.Override()
  public int getX();//  getX()

  @java.lang.Override()
  public int y();//  y()

  public final int getV();//  getV()
}

public final class InlinedDelegate /* InlinedDelegate*/<T>  {
  private T node;

  public final T getNode();//  getNode()
}

public final class UInt /* UInt*/ {
  private final int value;
}
