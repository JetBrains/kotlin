public @interface Ann {
    Class<?> value();
    int x() default 1;
    double y() default 1.0;
    Class<?> arg() default String;
    Class<?>[] args() default {};
}
