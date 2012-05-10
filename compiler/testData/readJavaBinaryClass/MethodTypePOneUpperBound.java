package test;

interface Foo {}

class MethodTypePOneUpperBound {
    <T extends Foo> void bar() {}
}
