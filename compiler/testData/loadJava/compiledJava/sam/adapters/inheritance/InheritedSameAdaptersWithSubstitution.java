package test;

import java.util.Comparator;

public interface InheritedSameAdaptersWithSubstitution {
    public interface Super1 {
        void foo(Comparator<String> r);
    }

    public interface Super2<T> {
        void foo(Comparator<T> r);
    }

    public interface Super2Substituted extends Super2<String> {
    }

    public interface Sub extends Super1, Super2Substituted {
    }
}
