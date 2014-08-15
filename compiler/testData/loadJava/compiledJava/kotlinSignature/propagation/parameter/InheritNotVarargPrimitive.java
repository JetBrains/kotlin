package test;

public interface InheritNotVarargPrimitive {

    public interface Super {
        void foo(int[] p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        void foo(int... p);
    }
}
