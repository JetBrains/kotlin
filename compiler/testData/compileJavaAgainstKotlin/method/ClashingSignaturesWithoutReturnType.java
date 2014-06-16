package test;

import java.util.List;

public class ClashingSignaturesWithoutReturnType {
    void test(List<String> ls, List<Integer> li) {
        K k = new K();
        k.foo(ls);
        k.foo(li);
    }
}
