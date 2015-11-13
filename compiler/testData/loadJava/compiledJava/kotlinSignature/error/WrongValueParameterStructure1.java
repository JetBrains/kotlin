package test;

import java.util.*;

public class WrongValueParameterStructure1 {
    public String foo(String a, List<Map.Entry<String, String>> b) {
        throw new UnsupportedOperationException();
    }
}
