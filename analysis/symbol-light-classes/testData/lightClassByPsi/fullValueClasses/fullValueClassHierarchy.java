public abstract class AbstractValueClass /* pack.AbstractValueClass*/ {
}

public final class FinalRegularClass /* pack.FinalRegularClass*/ extends pack.SealedValueClass {
  private final int abstractProperty;

  @java.lang.Override()
  public int getAbstractProperty();//  getAbstractProperty()

  @java.lang.Override()
  public int getCode();//  getCode()

  @java.lang.Override()
  public void abstractFunction();//  abstractFunction()

  public  FinalRegularClass(int);//  .ctor(int)
}

public final class FinalValueClass /* pack.FinalValueClass*/ extends pack.SealedValueClass {
  private final int first;

  private final int second;

  @java.lang.Override()
  public int getAbstractProperty();//  getAbstractProperty()

  @java.lang.Override()
  public int getCode();//  getCode()

  @java.lang.Override()
  public void abstractFunction();//  abstractFunction()

  @java.lang.Override()
  public void openFunction();//  openFunction()

  public final int getFirst();//  getFirst()

  public final int getSecond();//  getSecond()
}

public class OpenRegularClass /* pack.OpenRegularClass*/ extends pack.SealedValueClass {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.String mutable;

  private final int abstractProperty;

  @java.lang.Override()
  public int getAbstractProperty();//  getAbstractProperty()

  @java.lang.Override()
  public int getCode();//  getCode()

  @java.lang.Override()
  public void abstractFunction();//  abstractFunction()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getMutable();//  getMutable()

  public  OpenRegularClass(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  public final void setMutable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setMutable(@org.jetbrains.annotations.NotNull() java.lang.String)
}

public abstract class SealedValueClass /* pack.SealedValueClass*/ extends pack.AbstractValueClass {
}

public final class ValueObject /* pack.ValueObject*/ extends pack.SealedValueClass {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueObject INSTANCE;

  @java.lang.Override()
  public int getAbstractProperty();//  getAbstractProperty()

  @java.lang.Override()
  public int getCode();//  getCode()

  @java.lang.Override()
  public void abstractFunction();//  abstractFunction()
}
