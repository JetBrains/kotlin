package builders

inline fun call(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) init: () -> Unit) {
    return {
        init()
    }()
}

//SMAP
//lambda.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambda.2.kt
//builders/BuildersPackage$lambda_2$HASH$call$1
//*L
//1#1,18:1
//*E