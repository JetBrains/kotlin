package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class WrongTypeName1 {
    @KotlinSignature("fun foo(a : String) : Unit")
    public String foo(String a) {
        throw new UnsupportedOperationException();
    }
}
