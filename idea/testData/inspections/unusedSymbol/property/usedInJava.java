package test;

import foo.Obj;
import foo.UsedInJavaKt;

class usedInJava {
    public static void main(String[] args) {
        UsedInJavaKt.getUsedByGetter();
        UsedInJavaKt.setUsedBySetter(":|");
        System.out.println(Obj.getCONST());
    }
}