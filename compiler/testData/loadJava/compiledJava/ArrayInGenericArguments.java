public class ArrayInGenericArguments {
    public static class DataKey<T> {}

    public static final DataKey<String[]> X = null;
    public static final DataKey<int[]> Y = null;
    public static final DataKey<? extends CharSequence[]> Z = null;
}
