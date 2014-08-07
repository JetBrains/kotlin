package test;

import java.lang.Integer;
import java.lang.String;
import java.util.*;

public class Rendering {

    class A_Plain {}

    class B_Super {}

    class C_Sub extends B_Super {}

    class D_SuperG<T> {}

    class E_SubG extends D_SuperG<String> {}

    interface F_Array {
        void foo1(String[] strings);
        void foo2(List<String>[] strings);
        void foo3(Integer... args);
        void foo4(String... args);
        void foo5(List<String>... args);
    }

    interface G_Collections {
        void foo1(Iterator<String> x);
        void foo2(Iterable<String> x);
        void foo3(Collection<String> x);
        void foo4(List<String> x);
        void foo5(Set<String> x);
        void foo6(Map<String, String> x);
        void foo7(Map.Entry<String, String> x);
    }

    interface H_Raw {
        void foo1(List x);
        void foo2(D_SuperG x);
    }

    interface I_Wildcard {
        void foo1(List<? extends String> x);
        void foo2(List<? super String> x);
        void foo3(List<?> x);
        void foo4(D_SuperG<? extends String> x);
        void foo5(D_SuperG<? super String> x);
        void foo6(D_SuperG<?> x);
    }

}