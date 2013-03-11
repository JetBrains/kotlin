package test;

@interface A {
    B value();
}

@A(@B("test"))
@interface B {
    String value();
}
