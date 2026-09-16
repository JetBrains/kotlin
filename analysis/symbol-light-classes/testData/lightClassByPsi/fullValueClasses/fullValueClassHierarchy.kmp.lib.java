public abstract class AbstractValueClass /* pack.AbstractValueClass*/ {
}

public final class FinalRegularClass /* pack.FinalRegularClass*/ extends pack.SealedValueClass {
  private final int abstractProperty;

  public  FinalRegularClass(int);//  .ctor(int)

  public int getAbstractProperty();//  getAbstractProperty()

  public int getCode();//  getCode()

  public void abstractFunction();//  abstractFunction()
}

public final class FinalValueClass /* pack.FinalValueClass*/ extends pack.SealedValueClass {
  private final int first;

  private final int second;
}

public class OpenRegularClass /* pack.OpenRegularClass*/ extends pack.SealedValueClass {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.String mutable;

  private final int abstractProperty;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getMutable();//  getMutable()

  public  OpenRegularClass(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  public final void setMutable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setMutable(@org.jetbrains.annotations.NotNull() java.lang.String)

  public int getAbstractProperty();//  getAbstractProperty()

  public int getCode();//  getCode()

  public void abstractFunction();//  abstractFunction()
}

public abstract class SealedValueClass /* pack.SealedValueClass*/ extends pack.AbstractValueClass {
}

public final class ValueObject /* pack.ValueObject*/ extends pack.SealedValueClass {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueObject INSTANCE;
}
