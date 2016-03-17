package test;

import java.util.List;

public interface B1 {
    A<List<String>>.Inner<Integer, Double> produceA();

    List<A<String>> produceListOfAs();
}
