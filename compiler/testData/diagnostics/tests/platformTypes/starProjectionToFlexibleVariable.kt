// FIR_IDENTICAL
// SKIP_TXT
// FILE: ExtensionPointName.java
public final class ExtensionPointName<T> {}
// FILE: ExtensionPoint.java
import org.jetbrains.annotations.NotNull;

public interface ExtensionPoint<@NotNull T> {
    T foo();
}
// FILE: Area.java
public class Area {
    @NotNull
    public static <T> ExtensionPoint<T> getExtensionPoint(@NotNull ExtensionPointName<T> extensionPointName) { return null; }
}
// FILE: main.kt

private fun unregisterEverything(extensionPoint: ExtensionPointName<*>) {
    Area.getExtensionPoint(extensionPoint).foo().hashCode()
}
