import seal.*

class JavaSealedTest {
    Sealed sealedInsideClass = SubSealed.INSTANCE;

    public void testSeal() {
        Sealed sealedInsideMethod = SubSealed.INSTANCE;

        SubSealed.INSTANCE.toString();

        // Will be deleted because Java doesn't allow a expression to be used as a statement
    }
}