public class JavaClass {

    public static class C extends B {
        public OutPair<String, Integer> foo() {
            return super.foo();
        }

        public In<Object> bar() {
            return super.bar();
        }
    }

    public static String test() {
        A a = new C();

        if (!a.foo().getX().equals("OK")) return "fail 1";
        if (!a.foo().getY().equals(123)) return "fail 2";

        if (!a.bar().make("123").equals("123")) return "fail 3";

        return "OK";
    }
}
