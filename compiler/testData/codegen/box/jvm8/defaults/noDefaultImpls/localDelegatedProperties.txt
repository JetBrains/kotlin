@kotlin.Metadata
public final class Delegate {
    // source: 'localDelegatedProperties.kt'
    public method <init>(): void
    public final @org.jetbrains.annotations.NotNull method getValue(@org.jetbrains.annotations.Nullable p0: java.lang.Object, @org.jetbrains.annotations.NotNull p1: kotlin.reflect.KProperty): java.lang.String
}

@kotlin.Metadata
public interface Foo {
    // source: 'localDelegatedProperties.kt'
    public synthetic final static field $$delegatedProperties: kotlin.reflect.KProperty[]
    static method <clinit>(): void
    public @org.jetbrains.annotations.NotNull method test(): java.lang.String
}

@kotlin.Metadata
public final class LocalDelegatedPropertiesKt$box$1 {
    // source: 'localDelegatedProperties.kt'
    enclosing method LocalDelegatedPropertiesKt.box()Ljava/lang/String;
    inner (anonymous) class LocalDelegatedPropertiesKt$box$1
    method <init>(): void
}

@kotlin.Metadata
public final class LocalDelegatedPropertiesKt {
    // source: 'localDelegatedProperties.kt'
    inner (anonymous) class LocalDelegatedPropertiesKt$box$1
    public final static @org.jetbrains.annotations.NotNull method box(): java.lang.String
}
