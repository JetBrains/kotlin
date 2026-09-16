public abstract class AbstractValueClass /* pack.AbstractValueClass*/ {
  public  AbstractValueClass(int);//  .ctor(int)

  public abstract int getAbstractProperty();//  getAbstractProperty()

  public abstract void abstractFunction();//  abstractFunction()

  public final int getFinalProperty();//  getFinalProperty()

  public final void finalFunction();//  finalFunction()

  public int getOpenProperty();//  getOpenProperty()

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
  public java.lang.String toString();//  toString()

  public  FinalValueClass(int, int);//  .ctor(int, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

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
  private java.lang.String mutable;

  private final int abstractProperty;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getMutable();//  getMutable()

  public  OpenRegularClass(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, java.lang.String)

  public final void setMutable(@org.jetbrains.annotations.NotNull() java.lang.String);//  setMutable(java.lang.String)

  public int getAbstractProperty();//  getAbstractProperty()

  public int getCode();//  getCode()

  public void abstractFunction();//  abstractFunction()
}

public abstract class SealedValueClass /* pack.SealedValueClass*/ extends pack.AbstractValueClass {
  private  SealedValueClass(int);//  .ctor(int)

  public abstract int getCode();//  getCode()
}

public final class ValueObject /* pack.ValueObject*/ extends pack.SealedValueClass {
  @org.jetbrains.annotations.NotNull()
  public static final pack.ValueObject INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  private  ValueObject();//  .ctor()

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public int getAbstractProperty();//  getAbstractProperty()

  public int getCode();//  getCode()

  public int hashCode();//  hashCode()

  public void abstractFunction();//  abstractFunction()
}
