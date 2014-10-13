public class GenericUnit {
    public static class Key<T> {}

    public static <T> T getNull(Key<T> key) {
        return null;
    }

    public static <T> T get(Key<T> key, T t) {
        return t;
    }
}