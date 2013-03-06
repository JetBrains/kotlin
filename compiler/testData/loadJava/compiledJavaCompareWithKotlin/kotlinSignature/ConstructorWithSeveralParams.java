package test;

import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class ConstructorWithSeveralParams {
    @KotlinSignature("fun ConstructorWithSeveralParams(integer : Int, intField : Int, collection: ArrayList<String>)")
    public ConstructorWithSeveralParams(Integer integer, int intBasic, ArrayList<String> collection) {
    }
}