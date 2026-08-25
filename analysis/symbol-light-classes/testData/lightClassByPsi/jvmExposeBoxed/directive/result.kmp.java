public class OpenClass /* OpenClass*/ {
  public  OpenClass();//  .ctor()

  public void resultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  resultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

public final class Regular /* Regular*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.Object resultProp;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.Object getResultProp();//  getResultProp()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.Object resultInReturn();//  resultInReturn()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() java.lang.Object suspendResultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() kotlin.Unit>);//  suspendResultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object, @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() kotlin.Unit>)

  public  Regular();//  .ctor()

  public final void resultAndValueClassInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  resultAndValueClassInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object, @org.jetbrains.annotations.NotNull() java.lang.String)

  public final void resultInContext(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  resultInContext(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void resultInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  resultInExtension(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void resultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  resultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void setResultProp(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  setResultProp(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

public abstract interface RegularInterface /* RegularInterface*/ {
  public abstract void resultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  resultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

public final class ResultKt /* ResultKt*/ {
  public static final void topLevelResultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  topLevelResultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

@<error>()
public final class Some /* Some*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String value;

  @kotlin.jvm.JvmExposeBoxed()
  public  Some(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()
}
