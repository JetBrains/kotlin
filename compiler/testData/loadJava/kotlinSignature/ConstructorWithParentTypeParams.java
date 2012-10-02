package test;

import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class ConstructorWithParentTypeParams<T> {
    @KotlinSignature("fun ConstructorWithParentTypeParams(first: T)")
    public ConstructorWithParentTypeParams(T first) {
    }
}