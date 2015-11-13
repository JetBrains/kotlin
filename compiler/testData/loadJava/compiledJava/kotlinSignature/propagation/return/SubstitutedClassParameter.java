package test;

public interface SubstitutedClassParameter {

    public interface Super<T> {
        T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super<String> {
        String foo();
    }
}
