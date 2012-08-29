package annotations;

import annotations.MyEnum;

@interface MyAnnotation {
    MyEnum value();
}

@MyAnnotation(MyEnum.ONE)
class testClass {}
