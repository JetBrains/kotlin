import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.annotations.NotNull;

public class A {
    @NotNull
    public final String NULL = null;
    
    @NotNull
    public static final String STATIC_NULL = null;
    
    @KotlinSignature("fun foo() : String")
    public String foo() {
        return null;
    }
    
    @KotlinSignature("fun staticFoo() : String")
    public static String staticFoo() {
        return null;
    }
    
    @KotlinSignature("fun plus(a: A) : A")
    public A plus(A a) {
        return null;
    }

    @KotlinSignature("fun inc() : A")
    public A inc() {
        return null;
    }
    
    @KotlinSignature("fun get(o: Any) : Any")
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
