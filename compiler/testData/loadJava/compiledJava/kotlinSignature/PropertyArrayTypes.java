package test;

import java.lang.String;
import java.util.ArrayList;

import jet.runtime.typeinfo.KotlinSignature;

public class PropertyArrayTypes<T> {
    @KotlinSignature("var arrayOfArrays : Array<Array<String>>")
    public String[][] arrayOfArrays;

    @KotlinSignature("var array : Array<String>")
    public String[] array;

    @KotlinSignature("var genericArray : Array<T>")
    public T[] genericArray;
}
