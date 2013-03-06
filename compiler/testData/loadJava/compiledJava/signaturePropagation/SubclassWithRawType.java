package test;

import java.io.Serializable;
import java.lang.Comparable;
import java.lang.Runnable;
import java.util.List;

public interface SubclassWithRawType {
    interface Super {
        List simple1();
        List simple2();
        List<String> simple3();

        List<? extends List> boundWildcard1();
        List<? super List<String>> boundWildcard2();

        List<?> wildcard();

        List[] array1();
        List<String>[] array2();
    }

    interface Sub extends Super {
        List<String> simple1();
        List<List<String>> simple2();
        List simple3();

        List<? extends List<String>> boundWildcard1();
        List<? super List> boundWildcard2();

        List wildcard();

        List<String>[] array1();
        List[] array2();
    }
}
