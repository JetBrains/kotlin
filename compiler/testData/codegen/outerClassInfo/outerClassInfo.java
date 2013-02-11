package foo;

import java.lang.Object;

class Foo {

    class object { }

    class InnerClass { }

    class InnerObject { }

    void foo(Foo f) {
        class LocalClass {}
        class LocalObject { }
    }


    Object objectLiteral = new Object() {
        void objectLiteralFoo() { }
    };

}

class PackageInnerObject { }

class FooPackage {
    Object packageObjectLiteral = new Object() {
        void objectLiteralFoo() { }
    };

    void packageMethod(Foo f) {
        class PackageLocalClass {}
        class PackageLocalObject {}
    }
}
