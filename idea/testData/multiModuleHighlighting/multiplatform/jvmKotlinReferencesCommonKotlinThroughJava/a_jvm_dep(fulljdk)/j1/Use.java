package j1;

import common.A;

public class Use {
    public static A<String, String> returnA() {
        return new A<String, String>("", "");
    }

    public static void acceptA(A<String, String> a) {

    }
}