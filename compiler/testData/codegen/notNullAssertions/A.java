import org.jetbrains.annotations.NotNull;

public class A {
    @NotNull
    public final String NULL = null;
    
    @NotNull
    public static final String STATIC_NULL = null;
    
    public String foo() {
        return null;
    }
    
    public static String staticFoo() {
        return null;
    }
    
    public A plus(A a) {
        return null;
    }

    public A inc() {
        return null;
    }
    
    public Object get(Object o) {
        return null;
    }

    public A a() { return this; }

    public static class B {
        public static B b() { return null; }
    }

    public static class C {
        public static C c() { return null; }
    }
}
