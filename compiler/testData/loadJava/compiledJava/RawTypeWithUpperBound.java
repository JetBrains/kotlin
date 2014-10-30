package test;

import java.util.List;

public interface RawTypeWithUpperBound {

    public interface Foo<T extends CharSequence> {
    }

    interface Bar {
        void f(Foo f);
        void g(List<Foo> f);
    }
}
