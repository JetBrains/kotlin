package consumer;

public interface Consumer<T> {
    boolean consumer(T t);
}