public class JavaUser {
    // Dangerous
    @Ann(s = KotlinPropertiesKt.importantString)
    public void foo() {}

    // Also dangerous
    @AnnValue(OtherPropertiesKt.notSoImportantString)
    public void bar() {}

    // Safe
    @Ann(s = KotlinPropertiesKt.constantString)
    public void baz(String z) {
        switch (z) {
            case KotlinPropertiesKt.constantString: // Safe
                break;

            case KotlinPropertiesKt.importantString: // Dangerous
                break;

            default:
                break;
        }
    }

    public void fau() {
        byte b1 = KotlinPropertiesKt.importantNumber; // Dangerous
        byte b2 = KotlinPropertiesKt.constantNumber; // Safe (const)
        byte b3 = 0 + KotlinPropertiesKt.importantNumber; // Dangerous
        int b4 = 0 + KotlinPropertiesKt.importantNumber; // Safe (types are the same)
        b4 += KotlinPropertiesKt.importantNumber; // Safe (modification)
        long l1 = KotlinPropertiesKt.importantNumber; // Safe (type widening)
        b2 = KotlinPropertiesKt.importantNumber; // Dangerous
        b1 = KotlinPropertiesKt.constantNumber; // Safe (const)
        b3 = (0 + KotlinPropertiesKt.importantNumber); // Dangerous
        b3 = (byte) (KotlinPropertiesKt.importantNumber); // Safe (explicit cast)
        b3 = convert(KotlinPropertiesKt.importantNumber); // Safe (explicit cast)
    }

    public byte convert(int x) {
        return (byte) x;
    }

    byte importantNumber = KotlinPropertiesKt.importantNumber; // Also dangerous
}