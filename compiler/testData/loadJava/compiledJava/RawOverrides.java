package test;
import java.util.*;

public class RawOverrides {
    public interface A<T> {
        <E extends CharSequence> E foo(T x, List<? extends T> y);
    }

    public class B implements A {
        @Override
        public String foo(Object x, List y) {
            return null;
        }
    }

    public class C {
        <E extends CharSequence, F extends E> E bar(F x, List<Map<E, F>> y) {
            return null;
        }
    }

    public class D extends C {
        @Override
        public String bar(CharSequence x, List y) {
            return null;
        }
    }
}
