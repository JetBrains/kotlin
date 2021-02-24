// FIR_IDENTICAL
// KT-1647 Pattern matching doesn't work with generics

open class Abs
class Bar : Abs()

fun <F : Abs> patternMatchingAndGenerics(arg : F) : String {
    if(arg is Bar){
      return "Bar";
    }
    return "else";
}