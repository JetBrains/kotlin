public class JavaUser {
    // Dangerous
    @Ann(s = KotlinPropertiesKt.importantString)
    public void foo() {}

    // Also dangerous
    @AnnValue(OtherPropertiesKt.notSoImportantString)
    public void bar() {}

    // Safe
    @Ann(s = KotlinPropertiesKt.constantString)
    public void baz() {}
}