package annotations;

@MyAnnotation(MyEnum.ONE)
class MyTest {}

@interface MyAnnotation {
    MyEnum value();
}

enum MyEnum {
    ONE
}
