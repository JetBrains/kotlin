package test;

class JavaClass {
    void testMethod() {

        try {
            KotlinThrowsKt.kt();
        }
        catch (E1 e) {}

        try {
            KotlinThrowsKt.ktJvm();
        }
        catch (E2 e) {}
    }
}
