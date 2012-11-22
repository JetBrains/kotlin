package test;

public interface InheritNotVararg {

    public interface Super {
        void foo(String[] p);
    }

    public interface Sub extends Super {
        void foo(String... p);
    }
}
