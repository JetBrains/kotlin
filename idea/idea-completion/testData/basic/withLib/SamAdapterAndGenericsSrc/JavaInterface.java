package lib;

public interface JavaInterface<T> {
    <K> T execute(Task<T> task, K k);
}