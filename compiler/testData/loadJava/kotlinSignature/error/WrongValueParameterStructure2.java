package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class WrongValueParameterStructure2 {
    @KotlinSignature("fun foo(a : String, b : List<Map.Entry<String?>?>) : String")
    public String foo(String a, List<Map.Entry<String, String>> b) {
        throw new UnsupportedOperationException();
    }
}
