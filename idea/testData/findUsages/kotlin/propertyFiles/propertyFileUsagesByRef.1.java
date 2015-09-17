import org.jetbrains.annotations.PropertyKey;

class A {
    static String message(@PropertyKey(resourceBundle = "idea.testData.findUsages.kotlin.propertyFiles.propertyFileUsagesByRef.2") String key, Object... args) {
        return key;
    }

    void test() {
        message("foo.bar");
    }
}