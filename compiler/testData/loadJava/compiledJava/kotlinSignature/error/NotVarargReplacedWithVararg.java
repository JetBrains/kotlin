package test;

import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class NotVarargReplacedWithVararg {
    @ExpectLoadError("Parameter in method signature is not vararg, but in alternative signature it is vararg")
    @KotlinSignature("fun foo(vararg s : String)")
    public void foo(String s) {
    }
}
