public class Box<T> {
    private final T value;

    public Box(T value) {
        this.value = value;
    }

    public static <T> Box<T> create(T defaultValue) {
        return new Box(defaultValue);
    }

    public T getValue() {
        return value;
    }
}