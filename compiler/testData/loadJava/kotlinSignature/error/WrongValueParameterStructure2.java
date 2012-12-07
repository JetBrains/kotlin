package test;

import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class WrongValueParameterStructure2 {
    @ExpectLoadError("'jet.Map.Entry<jet.String, jet.String>' type in method signature has 2 type arguments, while 'Map.Entry<String?>' in alternative signature has 1 of them")
    @KotlinSignature("fun foo(a : String, b : List<Map.Entry<String?>?>) : String")
    public String foo(String a, List<Map.Entry<String, String>> b) {
        throw new UnsupportedOperationException();
    }
}
