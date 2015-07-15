import java.lang.Override;
import java.lang.String;

class A {
    int <caret>foo(String s) {
        return s.length() * 2;
    }
}

class B extends A {
    @Override
    int foo(String s) {
        return super.foo(s + "_");
    }
}

class Test {
    void test() {
        new A().foo("");
        new B().foo("");
        new X().foo("");
        new Y().foo("");
        new Z().foo("");
    }
}