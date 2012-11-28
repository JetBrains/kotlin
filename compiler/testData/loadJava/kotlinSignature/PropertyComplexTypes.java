package test;

import java.lang.String;
import java.util.ArrayList;

import jet.runtime.typeinfo.KotlinSignature;

public class PropertyComplexTypes<T> {
    @KotlinSignature("var genericType : T")
    public T genericType;

    @KotlinSignature("var listDefinedGeneric : ArrayList<String>")
    public ArrayList<String> listDefinedGeneric;

    @KotlinSignature("var listGeneric : ArrayList<T>")
    public ArrayList<T> listGeneric;

    @KotlinSignature("var listOfGenericList : ArrayList<ArrayList<T>>")
    public ArrayList<ArrayList<T>> listOfGenericList;
}
