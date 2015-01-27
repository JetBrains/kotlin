package test;

import foo.UsedInJava;

class usedInJava {
    public static void main(String[] args) {
        new UsedInJava("a", "b").getUsedByGetter();
        new UsedInJava("a", "b").setUsedBySetter(":|");
    }
}