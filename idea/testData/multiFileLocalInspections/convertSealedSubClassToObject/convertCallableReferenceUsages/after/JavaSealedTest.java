import seal.*

class JavaSealedTest {
    public void testNesting() {
        new SubSealed.Nested();
        Supplier<SubSealed.Nested> nestedSupplier = SubSealed.Nested::new;

        SubSealed.INSTANCE.internalFunction();
        Runnable noArgFunction = SubSealed.INSTANCE::internalFunction;
    }
}