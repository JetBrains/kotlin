class A {
    public void foo(int a, int b) {
        System.out.println(a);
    }
}

class D extends B implements Z {
    public void foo(int a, int b) {

    }
}

class U {
    void bar(A a) {
        a.foo(1, 2)
    }

    void bar(B b) {
        b.foo(3, 4)
    }

    void bar(C c) {
        c.foo(5, 6)
    }

    void bar(D d) {
        d.foo(7, 8)
    }

    void bar(Z z) {
        z.foo(9, 10)
    }
}