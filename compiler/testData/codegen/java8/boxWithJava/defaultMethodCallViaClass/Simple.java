interface Simple {
    default String test(String s) {
        return s + "K";
    }

    static String testStatic(String s) {
        return s + "K";
    }
}