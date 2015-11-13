package test;

public interface SubstitutedClassParameters {

    public interface Super1<T> {
        T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Super2<E> {
        E foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super1<String>, Super2<String> {
        String foo();
    }
}
