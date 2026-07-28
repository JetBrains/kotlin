public class OpenClass /* OpenClass*/ {
  public  OpenClass();//  .ctor()
}

public final class Regular /* Regular*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.Object resultProp;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer> getResultProp();//  getResultProp()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer> resultInReturn();//  resultInReturn()

  @kotlin.jvm.JvmExposeBoxed()
  public final void resultAndValueClassInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Some);//  resultAndValueClassInParameter(@org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>, @org.jetbrains.annotations.NotNull() Some)

  @kotlin.jvm.JvmExposeBoxed()
  public final void resultInContext(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>);//  resultInContext(@org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @kotlin.jvm.JvmExposeBoxed()
  public final void resultInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>);//  resultInExtension(@org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @kotlin.jvm.JvmExposeBoxed()
  public final void resultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>);//  resultInParameter(@org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @kotlin.jvm.JvmExposeBoxed()
  public final void setResultProp(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>);//  setResultProp(@org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>)

  public  Regular();//  .ctor()
}

public abstract interface RegularInterface /* RegularInterface*/ {
}

public final class ResultKt /* ResultKt*/ {
  @kotlin.jvm.JvmExposeBoxed()
  public static final void topLevelResultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>);//  topLevelResultInParameter(@org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>)
}

@kotlin.jvm.JvmInline()
public final class Some /* Some*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String value;

  @kotlin.jvm.JvmExposeBoxed()
  public  Some(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
