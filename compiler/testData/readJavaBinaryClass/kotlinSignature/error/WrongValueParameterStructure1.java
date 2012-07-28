package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class WrongValueParameterStructure1 {
    @KotlinSignature("fun foo(a : String<Int?>, b : List<Map.Entry<String?, String>?>) : String")
    public String foo(String a, List<Map.Entry<String, String>> b) {
        throw new UnsupportedOperationException();
    }
}
