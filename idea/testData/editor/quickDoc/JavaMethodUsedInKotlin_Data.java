import jet.runtime.typeinfo.KotlinSignature;

class Test {
    /**
     * Java Method
     */
    @KotlinSignature("fun foo(param: String): Array<out Any>")
    public static Object[] foo(String param) {
        return new Object[0];
    }
}