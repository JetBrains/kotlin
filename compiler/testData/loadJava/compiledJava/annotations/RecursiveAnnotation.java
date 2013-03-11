package test;

@B(@A("test"))
@interface A {
    String value();
}

@B(@A("test"))
@interface B {
    A value();
}
