class Usages {
    void foo(A a) {
        a.foo("");
        System.out.println(a.getA() + " " + a.getB());
        a.setB(12);
    }

    void foo(X x) {
        x.foo("");
        System.out.println(x.getA() + " " + x.getB());
        x.setB(12);
    }
}