package a;

class J {
    void bar() {
        Test t = new Test();
        APackage.test();
        APackage.test(t);
        System.out.println(APackage.getTEST());
        System.out.println(APackage.getTEST(t));
        APackage.setTEST("");
        APackage.setTEST(t, "");
    }
}
