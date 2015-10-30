public class Base {

    protected static String BASE_ONLY = "BASE";

    protected static String baseOnly() {
        return BASE_ONLY;
    }

    protected static String TEST = "BASE";

    protected static String test() {
        return TEST;
    }

    public static class Derived extends Base {
        protected static String TEST = "DERIVED";

        protected static String test() {
            return TEST;
        }
    }
}