import test.*

fun box(): String {
    var result = "fail"

    annotatedWith { result = "OK"; true }

    return result
}


inline fun test(z: () -> Unit) {
    z()
}


// FILE: 2.smap
//PROBLEM of KT-11478 in additional line mapping for default source (so 'single' was replaces with 'first' in SMAP class init):
//*L
//1#1,15:1
//17#1:19


//SMAP
//severalMappingsForDefaultFile.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 severalMappingsForDefaultFile.1.kt
//SeveralMappingsForDefaultFile_1Kt
//+ 2 severalMappingsForDefaultFile.2.kt
//test/SeveralMappingsForDefaultFile_2Kt
//*L
//1#1,58:1
//8#2:59
//4#2:60
//*E
//
//SMAP
//severalMappingsForDefaultFile.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 severalMappingsForDefaultFile.2.kt
//test/SeveralMappingsForDefaultFile_2Kt$annotatedWith2$1
//+ 2 severalMappingsForDefaultFile.2.kt
//test/SeveralMappingsForDefaultFile_2Kt
//+ 3 severalMappingsForDefaultFile.1.kt
//SeveralMappingsForDefaultFile_1Kt
//*L
//1#1,13:1
//26#1:27
//12#2,2:14
//4#2,11:14
//8#2:25
//14#2,12:14
//6#3:26
//*E