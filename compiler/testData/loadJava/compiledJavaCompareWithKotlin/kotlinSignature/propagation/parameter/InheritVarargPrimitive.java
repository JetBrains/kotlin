package test;

public interface InheritVarargPrimitive {

    public interface Super {
        void foo(int... p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        void foo(int[] p);
    }
}
