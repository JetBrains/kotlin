// FILE: DataKey.java
public class DataKey<T> {}

// FILE: Keys.java
public class Keys {
    public static final DataKey<String[]> X = null;
    public static final DataKey<String> Y = null;

    public static <T> T getData(DataKey<T> key) {
        return null;
    }
}

// FILE: main.kt
fun main() {
    // Keys.X type loaded as DataKey<error>
    Keys.getData(Keys.X)[0].length
    Keys.getData(Keys.Y).length
}
