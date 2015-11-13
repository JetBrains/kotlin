package test;

public interface SubstitutedClassParameter {

    public interface Super<T> {
        void foo(T p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super<String> {
        void foo(String p);
    }
}
