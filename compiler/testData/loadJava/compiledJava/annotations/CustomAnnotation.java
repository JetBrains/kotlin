package test;

@MyAnnotation(MyEnum.ONE)
class MyTest {}

@interface MyAnnotation {
    MyEnum value();
}

enum MyEnum {
    ONE
}
