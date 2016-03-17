package test;

import java.util.Collection;
import java.util.List;

public interface B2 {
    void consumeA(A<String, Integer, Double>.Inner<Collection<Number>> a);

    void consumeListOfAs(List<A<int[], Float, Boolean>> as);
}
