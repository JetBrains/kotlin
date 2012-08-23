package test;

import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;
import jet.*;

public class MethodWithTupleType {
    @KotlinSignature("fun foo(pair : #(String, String?))")
    public void foo(Tuple2<String, String> pair) {
    }
}
