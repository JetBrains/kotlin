package test;

public interface InheritNotVarargPrimitive {

    public interface Super {
        void foo(int[] p);
    }

    public interface Sub extends Super {
        void foo(int... p);
    }
}
