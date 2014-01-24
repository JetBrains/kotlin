package test;

class A {
    void bar() {
        TestPackage.foo(
                "", 10, new Function1<Integer, Unit>() {
                    public Unit invoke(Integer n) {
                        System.out.println(n);
                    }
                }
        );
    }
}