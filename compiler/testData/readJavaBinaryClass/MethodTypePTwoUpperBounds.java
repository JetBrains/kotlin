package test;

interface Foo {}
interface Bar {}

class MethodTypePTwoUpperBounds {
    <T extends Foo & Bar> void foo() {}
}
