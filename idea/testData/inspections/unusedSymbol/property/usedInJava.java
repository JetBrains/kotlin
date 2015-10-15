package test;

import foo.FooPackage;
import foo.Obj;

class usedInJava {
    public static void main(String[] args) {
        UsedInJavaKt.getUsedByGetter();
        UsedInJavaKt.setUsedBySetter(":|");
        System.out.println(Obj.CONST);
    }
}