@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Anno {
    final Anno(int i, int i1, @org.jetbrains.annotations.NotNull java.lang.String p, double v, @org.jetbrains.annotations.NotNull int[] ints, @org.jetbrains.annotations.NotNull int[] ints1);

    int i();

    int j() default 5;

    @org.jetbrains.annotations.NotNull
    java.lang.String value() default "a";

    double d() default 0.0;

    @org.jetbrains.annotations.NotNull
    int[] ia();

    @org.jetbrains.annotations.NotNull
    int[] ia2() default {1, 2, 3};
}