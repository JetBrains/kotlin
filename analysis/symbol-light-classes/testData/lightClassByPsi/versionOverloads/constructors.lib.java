public final class IntroducedAfterBase /* IntroducedAfterBase*/ {
  private final int a;

  private final long base;

  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  public  IntroducedAfterBase(long);//  .ctor(long)

  public  IntroducedAfterBase();//  .ctor()

  public  IntroducedAfterBase(long, @kotlin.IntroducedAt(version = "1") int);//  .ctor(long, int)

  public final int getA();//  getA()

  public final long getBase();//  getBase()
}

public final class IntroducedOnly /* IntroducedOnly*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String b;

  private final int a;

  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  public  IntroducedOnly();//  .ctor()

  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to version 1.", level = kotlin.DeprecationLevel.ERROR)
  public  IntroducedOnly(@kotlin.IntroducedAt(version = "1") int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getB();//  getB()

  public  IntroducedOnly(@kotlin.IntroducedAt(version = "1") int, @kotlin.IntroducedAt(version = "2") @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, java.lang.String)

  public final int getA();//  getA()
}

public final class Outer /* Outer*/ {
  public  Outer();//  .ctor()

  class Inner ...
}

public final class Inner /* Outer.Inner*/ {
  private final int a;

  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  public  Inner();//  .ctor()

  public  Inner(@kotlin.IntroducedAt(version = "1") int);//  .ctor(int)

  public final int getA();//  getA()
}

public final class Secondary /* Secondary*/ {
  private final long base;

  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  public  Secondary();//  .ctor()

  public  Secondary(@kotlin.IntroducedAt(version = "1") int);//  .ctor(int)

  public  Secondary(long);//  .ctor(long)

  public final long getBase();//  getBase()
}

public final class WithMandatoryBase /* WithMandatoryBase*/ {
  private final int a;

  private final long base;

  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  public  WithMandatoryBase(long);//  .ctor(long)

  public  WithMandatoryBase(long, @kotlin.IntroducedAt(version = "1") int);//  .ctor(long, int)

  public final int getA();//  getA()

  public final long getBase();//  getBase()
}
