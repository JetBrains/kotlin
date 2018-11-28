import seal.*

class JavaSealedTest {
    Sealed sealedInsideClass = new SubSealed();

    public void testSeal() {
        Sealed sealedInsideMethod = new SubSealed();

        new SubSealed().toString();

        // Will be deleted because Java doesn't allow a expression to be used as a statement
        new SubSealed();
    }
}