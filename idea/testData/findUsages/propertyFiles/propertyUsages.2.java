class A {
    void test() {
        @PropertyKey(resourceBundle = "idea.testData.findUsages.propertyFiles.propertyUsages.0") String s1 = "foo.bar"
        @PropertyKey(resourceBundle = "idea.testData.findUsages.propertyFiles.propertyUsages.0") String s2 = "foo.baz"
        PropertyUsages_1Kt.message("foo.bar");
        PropertyUsages_1Kt.message("foo.baz");
    }
}