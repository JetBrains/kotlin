package forTests.javaContext;

import java.util.ArrayList;

public class JavaClass {
    public void simple() {
        int breakpoint = 1;
    }

    public void localVariable() {
        int i = 1;
        int breakpoint = 1;
    }

    public void block() {
        int bodyVal = 1;
        if (true) {
            int thenVal = 1;
            int breakpoint = 1;
        }
        else {
            int elseVal = 1;
        }
    }

    public void imports() {
        ArrayList<Integer> list = createList();
        int breakpoint = 1;
    }

    private ArrayList<Integer> createList() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        list.add(1);
        list.add(2);
        return list;
    }

    public void markObject() {
        Integer i = 1;
        int breakpoint = 1;
    }

    public int javaProperty = 1;
    private int javaPrivateProperty = 1;

    public void property() {
        int breakpoint = 1;
    }

    public interface JavaStatic {
        static int state() { return 1; }
    }
}