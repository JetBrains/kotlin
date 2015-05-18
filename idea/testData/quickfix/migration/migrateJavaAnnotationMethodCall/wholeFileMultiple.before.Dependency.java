public @interface Ann {
    Class<?> value();
    int x() default 1;
    int y() default 2;
    Class<?> arg() default String;
    Class<?>[] args() default {};
}
