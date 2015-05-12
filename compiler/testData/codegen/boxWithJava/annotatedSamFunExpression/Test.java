import java.lang.Runnable;

class Test {
    public static Class<?> apply(Runnable x) {
        return x.getClass();
    }

    public static interface ABC {
        void apply(String x1, String x2);
    }

    public static Class<?> applyABC(ABC x) {
        return x.getClass();
    }
}
