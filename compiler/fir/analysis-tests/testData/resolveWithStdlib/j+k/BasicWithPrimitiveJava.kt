// FILE: Some.java

public class Some {
    public boolean foo(int param) {
        return param > 0;
    }

    public String[] bar(int[] arr) {
        String[] result = new String[arr.length];
        int i = 0;
        for (int elem: arr) {
            result[i++] = elem;
        }
        return result;
    }
}

// FILE: jvm.kt

class A : Some() {
    fun test() {
        val res1 = foo(1)
        val res2 = foo(-1)
        val res3 = bar(intArrayOf(0, 2, -2))
    }
}
