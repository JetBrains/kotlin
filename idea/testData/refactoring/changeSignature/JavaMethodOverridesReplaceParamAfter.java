import java.lang.Override;
import java.lang.String;

class A {
    String foo(int x) {
        return s.length() * 2;
    }
}

class B extends A {
    @Override
    String foo(int x) {
        return super.foo(x);
    }
}

class Test {
    void test() {
        new A().foo(1);
        new B().foo(1);
        new X().foo(1);
        new Y().foo(1);
        new Z().foo(1);
    }
}