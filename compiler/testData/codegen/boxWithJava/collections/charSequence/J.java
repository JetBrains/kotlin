import java.util.*;

public class J {

    public static class B extends A {
        public char get(int index) {
            if (index == 1) return 'a';
            return super.get(index);
        }
    }

    public static String foo() {
        B b = new B();
        CharSequence cs = (CharSequence) b;

        if (cs.charAt(0) != 'z') return "fail 1";
        if (b.get(0) != 'z') return "fail 2";

        if (cs.charAt(1) != 'a') return "fail 3";
        if (b.get(1) != 'a') return "fail 4";

        return "OK";
    }
}