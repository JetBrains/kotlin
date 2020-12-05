public class Open /* pkg.Open*/ {
  public  Open();//  .ctor()




  class Private ...
  
    class Private2 ...
  
    class StaticInternal ...
  
  }

private static final class Private /* pkg.Open.Private*/ extends pkg.Open {
  public  Private();//  .ctor()

}

protected final class Private2 /* pkg.Open.Private2*/ {
  public  Private2();//  .ctor()

}

public static final class StaticInternal /* pkg.Open.StaticInternal*/ {
  public  StaticInternal();//  .ctor()

}

public final class OuterInternal /* pkg.OuterInternal*/ {
  public  OuterInternal();//  .ctor()

}

final class TopLevelPrivate /* pkg.TopLevelPrivate*/ {
  public  TopLevelPrivate();//  .ctor()

}

public abstract class Season /* pkg.Season*/ {
  private  Season();//  .ctor()


  class Nested ...
  
  }

public static final class Nested /* pkg.Season.Nested*/ extends pkg.Season {
  public  Nested();//  .ctor()

}

public abstract class SealedWithArgs /* pkg.SealedWithArgs*/ {
  private final int a;

  private  SealedWithArgs(int);//  .ctor(int)

  public final int getA();//  getA()

}