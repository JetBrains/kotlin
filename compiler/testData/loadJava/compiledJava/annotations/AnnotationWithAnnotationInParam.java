package test;

@interface MyAnnotationWithParam {
    MyAnnotation value();
}

@interface MyAnnotation {
    String value();
}

@MyAnnotationWithParam(@MyAnnotation("test"))
class A {}

@interface MyAnnotation2 {
    String[] value();
}

@interface MyAnnotationWithParam2 {
    MyAnnotation2 value();
}

@MyAnnotationWithParam2(@MyAnnotation2({"test", "test2"}))
class B {}

@interface MyAnnotation3 {
    String first();
    String second();
}

@interface MyAnnotationWithParam3 {
    MyAnnotation3 value();
}

@MyAnnotationWithParam3(@MyAnnotation3(first = "f", second = "s"))
class C {}
