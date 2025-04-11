package test;

public @interface Test {
    String value() default "";
    int count() default 0;
}