public final class TypeHierarchyMap <TValue> implements java.util.Map<java.lang.Class<?>,TValue>, kotlin.collections.Map<java.lang.Class<?>,TValue>, kotlin.jvm.internal.markers.KMappedMarker {
    public TypeHierarchyMap() { /* compiled code */ }

    public boolean containsKey(@org.jetbrains.annotations.NotNull java.lang.Class aClass) { /* compiled code */ }

    public final boolean containsKey(K k) { /* compiled code */ }

    @org.jetbrains.annotations.NotNull
    public final java.util.Set<java.util.Map.Entry<java.lang.Class<?>,TValue>> entrySet() { /* compiled code */ }

    @org.jetbrains.annotations.NotNull
    public static abstract java.util.Set<java.util.Map.Entry<java.lang.Class<?>,TValue>> $$<get-entries> /* Real name is '<get-entries>' */();

    @org.jetbrains.annotations.NotNull
    public final java.util.Set<java.lang.Class<?>> keySet() { /* compiled code */ }

    @org.jetbrains.annotations.NotNull
    public static abstract java.util.Set<java.lang.Class<?>> $$<get-keys> /* Real name is '<get-keys>' */();

    public final int size() { /* compiled code */ }

    public static abstract int $$<get-size> /* Real name is '<get-size>' */();

    @org.jetbrains.annotations.NotNull
    public final java.util.Collection<TValue> values() { /* compiled code */ }

    @org.jetbrains.annotations.NotNull
    public static abstract java.util.Collection<TValue> $$<get-values> /* Real name is '<get-values>' */();

    public static abstract boolean containsValue(TValue tValue);

    @org.jetbrains.annotations.Nullable
    public final V get(K k) { /* compiled code */ }

    @org.jetbrains.annotations.Nullable
    public static abstract TValue get(@org.jetbrains.annotations.NotNull java.lang.Class<?> aClass);
}