class C<W extends I> {
    abstract class <caret>B<X extends I, Y> extends A<X, I, Z<Y>> {
        // INFO: {"checked": "true"}
        <S extends X> void foo(X x1, Z<X> x2, Y y1, Z<Y> y2, W w1, Z<W> w2, S s1, Z<S> s2) {

        }
        // INFO: {"checked": "true"}
        class Foo<S> extends A<X, I, Z<Y>> implements Z<W> {

        }
        // INFO: {"checked": "true"}
        X foo1;
        // INFO: {"checked": "true"}
        Z<X> foo2;
        // INFO: {"checked": "true"}
        Y foo3;
        // INFO: {"checked": "true"}
        Z<Y> foo4;
    }
}
