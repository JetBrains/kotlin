interface Test {

    String call();

    default String test() {
        return "K";
    }

    static String testStatic() {
        return "K";
    }
}
