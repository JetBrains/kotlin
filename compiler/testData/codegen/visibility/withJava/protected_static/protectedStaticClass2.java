import java.lang.String;

public class protectedStaticClass2 {
    public static class A {
        protected static class B {
            public B() {
            }

            public String foo() {
                return "OK";
            }
        }
    }
}
