import java.util.*;

public class J {

    private static class MyMap<K, V> extends KMap<K, V> {}

    public static String foo() {
        Map<String, Integer> collection = new MyMap<String, Integer>();
        if (!collection.containsKey("ABCDE")) return "fail 1";
        if (!collection.containsValue(1)) return "fail 2";
        return "OK";
    }
}