// FIR_IDENTICAL
//KT-1602
import lib.ArrayFactory

public class Impl : ArrayFactory {
    <caret>
}

val array: Array<String> = emptyArray()