public static abstract interface INestedInternal /* pkg.Open.INestedInternal*/ {
}

static abstract interface INestedPrivate /* pkg.Open.INestedPrivate*/ {
}

public static abstract interface INestedProtected /* pkg.Open.INestedProtected*/ {
}

public static abstract interface INestedPublic /* pkg.Open.INestedPublic*/ {
}

public abstract interface ITopLevelInternal /* pkg.ITopLevelInternal*/ {
}

abstract interface ITopLevelPrivate /* pkg.ITopLevelPrivate*/ {
}

public abstract interface ITopLevelPublic /* pkg.ITopLevelPublic*/ {
}

public static final class Nested /* pkg.Season.Nested*/ extends pkg.Season {
  public  Nested();//  .ctor()
}

public class Open /* pkg.Open*/ {
  public  Open();//  .ctor()

  class INestedInternal ...

  class INestedPrivate ...

  class INestedProtected ...

  class INestedPublic ...

  class Private ...

  class Private2 ...

  class StaticInternal ...
}

public final class OuterInternal /* pkg.OuterInternal*/ {
  public  OuterInternal();//  .ctor()
}

private static final class Private /* pkg.Open.Private*/ extends pkg.Open {
  public  Private();//  .ctor()
}

protected final class Private2 /* pkg.Open.Private2*/ {
  public  Private2();//  .ctor()
}

public abstract class SealedWithArgs /* pkg.SealedWithArgs*/ {
  private final int a;

  protected  SealedWithArgs(int);//  .ctor(int)

  public final int getA();//  getA()
}

public abstract class Season /* pkg.Season*/ {
  protected  Season();//  .ctor()

  class Nested ...
}

public static final class StaticInternal /* pkg.Open.StaticInternal*/ {
  public  StaticInternal();//  .ctor()
}

final class TopLevelPrivate /* pkg.TopLevelPrivate*/ {
  public  TopLevelPrivate();//  .ctor()
}
