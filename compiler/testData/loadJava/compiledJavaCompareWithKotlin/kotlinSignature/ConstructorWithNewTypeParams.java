package test;

import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class ConstructorWithNewTypeParams<T> {
    @KotlinSignature("fun ConstructorWithNewTypeParams(first : Any)") // TODO: first : U doesn't work
    public <U>ConstructorWithNewTypeParams(U first) {
    }
}