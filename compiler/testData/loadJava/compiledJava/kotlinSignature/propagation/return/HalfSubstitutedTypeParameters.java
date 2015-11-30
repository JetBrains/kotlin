package test;

import java.util.*;

public interface HalfSubstitutedTypeParameters {

    public interface TrickyList<X, E> extends List<E> {}

    public interface Super {
        List<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        TrickyList<Integer, String> foo();
    }
}
