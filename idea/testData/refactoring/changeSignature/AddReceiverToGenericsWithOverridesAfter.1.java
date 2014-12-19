interface J<A> extends T<A> {
    @Override
    <B> int foofoofoo(U<A> a, B b);
}

abstract class J1<X> implements J<U<X>> {
    @Override
    public <C> int foofoofoo(U<U<X>> xu, C c) {
        throw new UnsupportedOperationException();
    }
}

abstract class J2 extends J1<String> {
    @Override
    public <C> int foofoofoo(U<U<String>> xu, C c) {
        throw new UnsupportedOperationException();
    }
}