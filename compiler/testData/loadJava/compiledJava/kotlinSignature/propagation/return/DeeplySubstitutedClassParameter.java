package test;

public interface DeeplySubstitutedClassParameter {

    public interface Super<T> {
        T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Middle<E> extends Super<E> {
        E foo();
    }

    public interface Sub extends Middle<String> {
        String foo();
    }
}
