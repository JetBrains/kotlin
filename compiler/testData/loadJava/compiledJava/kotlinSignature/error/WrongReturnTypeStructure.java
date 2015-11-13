package test;

import java.util.*;

public class WrongReturnTypeStructure {
    public String foo(String a, List<Map.Entry<String, String>> b) {
        throw new UnsupportedOperationException();
    }
}
