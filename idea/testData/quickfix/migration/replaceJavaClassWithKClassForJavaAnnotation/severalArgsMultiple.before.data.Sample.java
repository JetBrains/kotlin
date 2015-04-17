public @interface Ann {
    Class<?> value();
    int x();
    double y() default 1.0;
    Class<?> arg();
    Class<?>[] args();
}
