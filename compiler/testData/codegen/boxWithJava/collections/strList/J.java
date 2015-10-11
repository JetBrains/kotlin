import java.util.*;

public class J {

    private static class MyList extends KList {}

    public static String foo() {
        Collection<String> collection = new MyList();
        if (!collection.contains("ABCDE")) return "fail 1";
        if (!collection.containsAll(Arrays.asList(1, 2, 3))) return "fail 2";
        return "OK";
    }
}