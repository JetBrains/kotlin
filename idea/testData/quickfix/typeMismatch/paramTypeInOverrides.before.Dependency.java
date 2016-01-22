// "Change parameter 'a' type of function 'test.B.foo' to 'String'" "true"
package test;

class J extends B {
    @Override
    void foo(int a) {
        super.foo(a);
    }
}