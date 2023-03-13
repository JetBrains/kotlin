// SKIP_KLIB_TEST
// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR
import java.util.Date

val unitFun = { }
val intFun = { 42 }
val stringParamFun = { x: String -> }
val listFun = { l: List<String> -> l }
val mutableListFun = fun (l: MutableList<Double>): MutableList<Int> = null!!
val funWithIn = fun (x: Comparable<String>) {}

val extensionFun = fun Any.() {}
val extensionWithArgFun = fun Long.(x: Any): Date = Date()
