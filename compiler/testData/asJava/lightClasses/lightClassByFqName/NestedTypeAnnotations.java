public final class Nested /* foo.Nested*/ {
  @org.jetbrains.annotations.Nullable()
  private @foo.MyAnno(s = "outer") java.util.List<? extends @foo.MyAnno(s = "middle") java.util.List<@foo.AnotherAnnotation(k = foo.Nested.class) java.lang.String>> property = null /* initializer type: null */;

  @org.jetbrains.annotations.Nullable()
  public final @foo.MyAnno(s = "outer") java.util.List<@foo.MyAnno(s = "middle") java.util.List<@foo.AnotherAnnotation(k = foo.Nested.class) java.lang.String>> function(@org.jetbrains.annotations.NotNull() @foo.MyAnno(s = "outer") java.util.List<? extends @foo.MyAnno(s = "middle") java.util.List<@foo.AnotherAnnotation(k = foo.Nested.class) java.lang.String>>, @org.jetbrains.annotations.NotNull() @foo.MyAnno(s = "outer") java.util.List<? extends @foo.MyAnno(s = "middle") java.util.List<@foo.AnotherAnnotation(k = foo.Nested.class) java.lang.String>>);//  function(@foo.MyAnno(s = "outer") java.util.List<? extends @foo.MyAnno(s = "middle") java.util.List<@foo.AnotherAnnotation(k = foo.Nested.class) java.lang.String>>, @foo.MyAnno(s = "outer") java.util.List<? extends @foo.MyAnno(s = "middle") java.util.List<@foo.AnotherAnnotation(k = foo.Nested.class) java.lang.String>>)

  @org.jetbrains.annotations.Nullable()
  public final @foo.MyAnno(s = "outer") java.util.List<@foo.MyAnno(s = "middle") java.util.List<@foo.AnotherAnnotation(k = foo.Nested.class) java.lang.String>> getProperty();//  getProperty()

  public  Nested();//  .ctor()

  public final void setProperty(@org.jetbrains.annotations.Nullable() @foo.MyAnno(s = "outer") java.util.List<? extends @foo.MyAnno(s = "middle") java.util.List<@foo.AnotherAnnotation(k = foo.Nested.class) java.lang.String>>);//  setProperty(@foo.MyAnno(s = "outer") java.util.List<? extends @foo.MyAnno(s = "middle") java.util.List<@foo.AnotherAnnotation(k = foo.Nested.class) java.lang.String>>)
}
