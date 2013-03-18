package test;

public interface Comparator<T> {
    int compare(T o1, T o2);

    boolean equals(Object obj);
}
