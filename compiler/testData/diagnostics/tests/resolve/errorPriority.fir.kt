// SKIP_TXT

// FILE: MyJClass.java

public class MyJClass {
    public void meth(int pathname) {}
    private void meth(String pathname, int prefixLength) {}
    private void meth(String child, boolean b) {}
}

// FILE: MyJClass2.java

public class MyJClass2 {
    public void meth(int pathname) {}
    public void meth(String pathname, int prefixLength) {}
    private void meth(String child, boolean b) {}
}

// FILE: test.kt

fun test1(myJClass: MyJClass) {
    myJClass.meth(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
}

fun test2(myJClass: MyJClass2) {
    myJClass.meth(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
}