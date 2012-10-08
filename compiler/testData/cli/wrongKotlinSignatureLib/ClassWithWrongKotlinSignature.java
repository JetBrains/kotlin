import jet.runtime.typeinfo.KotlinSignature;

public class ClassWithWrongKotlinSignature {
    @KotlinSignature("fun bar() : String")
    public static String foo() {
        throw new UnsupportedOperationException();
    }
}
