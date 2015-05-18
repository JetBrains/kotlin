public @interface Ann {
    Class<? extends A>[] value();
    Class<? extends A.B> arg1();
    Class<? extends java.util.Random> arg2();
}

class A {
    static class B {}
}


