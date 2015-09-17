import org.jetbrains.annotations.PropertyKey;

class J {
    static String message(@PropertyKey(resourceBundle = "TestBundle") String key, Object... args) {
        return key;
    }
}