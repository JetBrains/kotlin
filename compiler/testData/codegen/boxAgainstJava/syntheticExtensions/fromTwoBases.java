interface A {
    String getOk();
}

interface B {
    String getOk();
}

interface C extends A, B {
}

class JavaClass implements C {
    public String getOk() { return "OK"; }
}
