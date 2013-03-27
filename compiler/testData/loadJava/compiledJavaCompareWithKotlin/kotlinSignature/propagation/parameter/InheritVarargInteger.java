package test;

public interface InheritVarargInteger {

    public interface Super {
        void foo(Integer... p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        void foo(Integer[] p);
    }
}
