import jet.runtime.typeinfo.KotlinSignature;

public class A {
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
}
