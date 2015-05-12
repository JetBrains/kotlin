import java.lang.Runnable;

class Test {
    public static Class<?> apply(Runnable x) {
        return x.getClass();
    }
}
