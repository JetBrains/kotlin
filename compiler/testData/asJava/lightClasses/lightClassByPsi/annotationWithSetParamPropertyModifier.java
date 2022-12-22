@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Anno /* Anno*/ {
}

public final class TestClass /* TestClass*/ {
  private int hello;

  public  TestClass(int);//  .ctor(int)

  public final int getHello();//  getHello()

  public final void setHello(@Anno() int);//  setHello(int)

}