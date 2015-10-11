import java.util.*;

public class J {

    public static class B extends A {
        public int getLength() { return 456; }
        public char get(int index) {
            if (index == 1) return 'a';
            return super.get(index);
        }
    }

    public static String foo() {
        B b = new B();
        CharSequence cs = (CharSequence) b;

        if (cs.length() != 456) return "fail 01";
        if (b.length() != 456) return "fail 02";
        if (b.getLength() != 456) return "fail 03";

        if (cs.charAt(0) != 'z') return "fail 1";
        if (b.get(0) != 'z') return "fail 2";

        if (cs.charAt(1) != 'a') return "fail 3";
        if (b.get(1) != 'a') return "fail 4";

        return "OK";
    }
}