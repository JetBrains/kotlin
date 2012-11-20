import jet.runtime.typeinfo.KotlinSignature;

public class ClassWithWrongKotlinSignatures {
    @KotlinSignature("fun bar() : String")
    public static String foo() {
        throw new UnsupportedOperationException();
    }

    @KotlinSignature("fun foo() : String")
    public static String bar() {
        throw new UnsupportedOperationException();
    }
}
