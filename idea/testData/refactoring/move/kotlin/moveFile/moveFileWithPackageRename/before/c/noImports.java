package c;

class J {
    void bar() {
        a.Test t = new a.Test();
        a.APackage.test();
        a.APackage.test(t);
        System.out.println(a.APackage.getTEST());
        System.out.println(a.APackage.getTEST(t));
        a.APackage.setTEST("");
        a.APackage.setTEST(t, "");
    }
}
