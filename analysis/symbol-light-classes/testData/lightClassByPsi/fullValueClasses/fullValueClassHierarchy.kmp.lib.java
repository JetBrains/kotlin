public abstract class AbstractValueClass /* pack.AbstractValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public  AbstractValueClass(int);//  .ctor(int)

  public abstract int getAbstractProperty();//  getAbstractProperty()

  public abstract void abstractFunction();//  abstractFunction()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getFinalProperty();//  getFinalProperty()

  public final void finalFunction();//  finalFunction()

  public int getOpenProperty();//  getOpenProperty()

  public int hashCode();//  hashCode()

  public void openFunction();//  openFunction()
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

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public  FinalValueClass(int, int);//  .ctor(int, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getFirst();//  getFirst()

  public final int getSecond();//  getSecond()

  public int getAbstractProperty();//  getAbstractProperty()

  public int getCode();//  getCode()

  public int hashCode();//  hashCode()

  public void abstractFunction();//  abstractFunction()

  public void openFunction();//  openFunction()
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
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  private  SealedValueClass(int);//  .ctor(int)

  public abstract int getCode();//  getCode()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class ValueObject /* pack.ValueObject*/ extends pack.SealedValueClass {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueObject INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  private  ValueObject();//  .ctor()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int getAbstractProperty();//  getAbstractProperty()

  public int getCode();//  getCode()

  public int hashCode();//  hashCode()

  public void abstractFunction();//  abstractFunction()
}
