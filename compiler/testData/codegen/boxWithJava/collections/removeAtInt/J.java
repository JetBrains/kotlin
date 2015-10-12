import java.util.*;

public class J {

    private static class MyList extends A {}

    public static String foo() {
        MyList myList = new MyList();
        List<Integer> list = (List<Integer>) myList;

        if (!list.remove((Integer) 1)) return "fail 1";
        if (list.remove((int) 1) != 123) return "fail 2";

        if (!myList.remove((Integer) 1)) return "fail 3";
        if (myList.remove((int) 1) != 123) return "fail 4";

        if (myList.removeAt(1) != 123) return "fail 5";
        return "OK";
    }
}
