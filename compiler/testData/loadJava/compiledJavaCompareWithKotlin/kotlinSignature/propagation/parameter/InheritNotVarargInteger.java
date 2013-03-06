package test;

public interface InheritNotVarargInteger {

    public interface Super {
        void foo(Integer[] p);
    }

    public interface Sub extends Super {
        void foo(Integer... p);
    }
}
