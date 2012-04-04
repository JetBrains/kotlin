import testData.libraries.*

fun foo() {
    println(testData.libraries.globalVal)
    println(globalValWithGetter)
}

//public val <1>globalVal : #(Int, String) = #(239, "239")
//
//public val <2>globalValWithGetter : Long