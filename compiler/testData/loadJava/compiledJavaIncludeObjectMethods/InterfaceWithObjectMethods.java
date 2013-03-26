package test;

public interface InterfaceWithObjectMethods {
    int hashCode();
    boolean equals(Object obj);
    Object clone();
    String toString();
    void finalize();
}