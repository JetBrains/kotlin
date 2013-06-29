class JavaClass {
    public static String run(Runnable r) {
        return r == null ? "OK" : "FAIL";
    }
}