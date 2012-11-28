package test;

public interface InheritVarargPrimitive {

    public interface Super {
        void foo(int... p);
    }

    public interface Sub extends Super {
        void foo(int[] p);
    }
}
