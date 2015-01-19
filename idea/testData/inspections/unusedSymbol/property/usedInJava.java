package test;

import foo.FooPackage;
import foo.Obj;

class usedInJava {
    public static void main(String[] args) {
        FooPackage.getUsedByGetter();
        FooPackage.setUsedBySetter(":|");
        System.out.println(Obj.CONST);
    }
}