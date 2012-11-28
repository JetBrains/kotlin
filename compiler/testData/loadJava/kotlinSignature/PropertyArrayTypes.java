package test;

import java.lang.String;
import java.util.ArrayList;

import jet.runtime.typeinfo.KotlinSignature;

public class PropertyArrayTypes<T> {
    @KotlinSignature("var arrayOfArrays : Array<out Array<out String>>")
    public String[][] arrayOfArrays;

    @KotlinSignature("var array : Array<out String>")
    public String[] array;

    @KotlinSignature("var genericArray : Array<out T>")
    public T[] genericArray;
}
