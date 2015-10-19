package test;

class GenericProperty {
    void foo() {
        java.util.Map<String, Integer> o = GenericPropertyKt.getTest(new java.util.HashMap<Integer, String>());
    }
}
