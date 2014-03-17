package b;

class J {
    void bar() {
        b.BPackage.setTest("");
        System.out.println(b.BPackage.getTest());
    }
}
