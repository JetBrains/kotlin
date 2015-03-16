package foo;

import java.lang.Object;

class Foo {

    class Companion { }

    class InnerClass { }

    class InnerObject { }

    void foo(Foo f) {
        class LocalClass {}
        class LocalObject { }
    }


    Object objectLiteral = new Object() {
        void objectLiteralFoo() { }
    };

    //anonymous lambda
    Foo() {
        class LambdaInConstructor{}
    }


    void foo() {
        //lambda
        class Lambda {}
    }
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
