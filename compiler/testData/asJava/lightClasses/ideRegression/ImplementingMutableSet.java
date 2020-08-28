public final class SmartSet <T> extends kotlin.collections.AbstractSet<T> implements java.util.Set<T>, kotlin.collections.MutableSet<T>, kotlin.jvm.internal.markers.KMutableSet {
    private java.lang.Object data;
    private int size;
    private static final int ARRAY_THRESHOLD;
    @org.jetbrains.annotations.NotNull
    public static final SmartSet.Companion Companion;

    public int getSize() { /* compiled code */ }

    public void setSize(int i) { /* compiled code */ }

    @org.jetbrains.annotations.NotNull
    public java.util.Iterator<T> iterator() { /* compiled code */ }

    public boolean add(T element) { /* compiled code */ }

    public void clear() { /* compiled code */ }

    private SmartSet() { /* compiled code */ }

    @kotlin.jvm.JvmStatic
    @org.jetbrains.annotations.NotNull
    public static final <T> SmartSet<T> create() { /* compiled code */ }

    @kotlin.jvm.JvmStatic
    @org.jetbrains.annotations.NotNull
    public static final <T> SmartSet<T> create(@org.jetbrains.annotations.NotNull java.util.Collection<? extends T> set) { /* compiled code */ }

    public static final class Companion {
        @kotlin.jvm.JvmStatic
        @org.jetbrains.annotations.NotNull
        public final <T> SmartSet<T> create() { /* compiled code */ }

        @kotlin.jvm.JvmStatic
        @org.jetbrains.annotations.NotNull
        public final <T> SmartSet<T> create(@org.jetbrains.annotations.NotNull java.util.Collection<? extends T> set) { /* compiled code */ }

        private Companion() { /* compiled code */ }
    }
}
