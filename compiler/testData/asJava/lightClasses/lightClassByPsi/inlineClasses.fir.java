public enum Foo /* Foo*/ {
  ;

  private final int x;

  public final int getX();//  getX()
}

public final class InlineInheritance /* InlineInheritance*/ {
  private final int v;

  public final int getV();//  getV()

  public int getX();//  getX()

  public int y();//  y()
}

public final class InlinedDelegate /* InlinedDelegate*/<T>  {
  private T node;

  public final T getNode();//  getNode()
}

public final class UInt /* UInt*/ {
  private final int value;
}
