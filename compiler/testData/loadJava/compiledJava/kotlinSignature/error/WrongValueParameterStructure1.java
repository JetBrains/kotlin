package test;

import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class WrongValueParameterStructure1 {
    //@ExpectLoadError("'kotlin.String?' type in method signature has 0 type arguments, while 'String<Int?>' in alternative signature has 1 of them")
    @KotlinSignature("fun foo(a : String<Int?>, b : List<Map.Entry<String?, String>?>) : String")
    public String foo(String a, List<Map.Entry<String, String>> b) {
        throw new UnsupportedOperationException();
    }
}
