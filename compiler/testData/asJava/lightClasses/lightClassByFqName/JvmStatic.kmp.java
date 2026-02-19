public final class A /* A*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() A.Companion Companion;

  public  A();//  .ctor()

  public static abstract interface I /* A.I*/ {
    @org.jetbrains.annotations.NotNull()
    public static final @org.jetbrains.annotations.NotNull() A.I.Companion Companion;

    public static final class C /* A.I.C*/ {
      @org.jetbrains.annotations.NotNull()
      public static final @org.jetbrains.annotations.NotNull() A.I.C INSTANCE;

      @<error>()
      public final void i();//  i()

      private  C();//  .ctor()
    }

    public static final class Companion /* A.I.Companion*/ {
      @<error>()
      public final void h();//  h()

      private  Companion();//  .ctor()
    }
  }

  public static final class B /* A.B*/ {
    @org.jetbrains.annotations.NotNull()
    public static final @org.jetbrains.annotations.NotNull() A.B INSTANCE;

    @<error>()
    public final void g();//  g()

    private  B();//  .ctor()
  }

  public static final class Companion /* A.Companion*/ {
    @<error>()
    public final void f();//  f()

    private  Companion();//  .ctor()
  }
}
