package test;

import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class VarargReplacedWithNotVararg {
    @ExpectLoadError("Parameter in method signature is vararg, but in alternative signature it is not")
    @KotlinSignature("fun foo(s : String)")
    public void foo(String... s) {
    }
}
