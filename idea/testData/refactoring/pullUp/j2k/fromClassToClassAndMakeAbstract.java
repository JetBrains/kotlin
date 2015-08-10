abstract class <caret>B extends A {
    // INFO: {"checked": "true", "toAbstract": "true"}
    int x = 2*3;
    // INFO: {"checked": "true", "toAbstract": "true"}
    static String X = "1" + "2";
    // INFO: {"checked": "true", "toAbstract": "true"}
    boolean foo(int n) {
        return n > 0;
    }
    // INFO: {"checked": "true", "toAbstract": "true"}
    static String foo2(int n) {
        return "_" + n + "_";
    }
    // INFO: {"checked": "true", "toAbstract": "true"}
    abstract int bar(String s);
    // INFO: {"checked": "true", "toAbstract": "true"}
    class X {

    }
    // INFO: {"checked": "true", "toAbstract": "true"}
    static class Y {

    }
}