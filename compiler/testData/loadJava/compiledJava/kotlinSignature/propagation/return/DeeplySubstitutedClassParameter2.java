package test;

public interface DeeplySubstitutedClassParameter2 {

    public interface Super<T> {
        T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Middle<E> extends Super<E> {
    }

    public interface Sub extends Middle<String> {
        String foo();
    }
}
