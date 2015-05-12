//KT-4415 Class Auto-Cast Bug

interface SelfJson

object A {
    fun find(<!UNUSED_PARAMETER!>clz<!>:Class<*>){  }

    fun toJson2(obj:Any){
        if(obj is SelfJson){
            // A.find( (obj as SelfJson).javaClass)  // OK
            A.find( <!DEBUG_INFO_SMARTCAST!>obj<!>.javaClass )   // ERROR:  Type mismatch: inferred type is kotlin.Any but SelfJson was expected
        }
    }
}

//from library
val <T> T.javaClass : Class<T> get() = throw Exception()
