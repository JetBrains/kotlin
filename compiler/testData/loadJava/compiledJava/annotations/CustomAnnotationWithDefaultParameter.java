package test;

@MyAnnotation(first = "f", second = "s")
class MyTest {}

@interface MyAnnotation {
    String first();
    String second() default("s");
}

