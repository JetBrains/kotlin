public class JavaTriesToExtendKotlinSealed {

    private static class TryToImplement implements KotlinSealedInterface {}
    private static interface TryToExtend extends KotlinSealedInterface {}
    private static class TryToExtendClass extends KotlinSealedClass {}

    class OkToImplement implements KotlinInterface {}
    interface OkToExtend extends KotlinInterface {}
    class OkToExtendClass extends KotlinClass{}

    public <OkTypeParam extends KotlinSealedClass> void getSealed() {}

    public static void main(String[] args) {
        KotlinSealedInterface sealedInterface = new KotlinSealedInterface() {}; // anonymouns class implements interface
        KotlinSealedClass sealedClass = new KotlinSealedClass() {};

        new KotlinInterface() {};
        new KotlinClass() {};
    }
}