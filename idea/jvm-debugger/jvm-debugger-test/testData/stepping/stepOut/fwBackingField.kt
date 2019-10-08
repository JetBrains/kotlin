package fwBackingField

class A {
    //FieldWatchpoint! (propVal)
    val propVal = 1

    //FieldWatchpoint! (propVar)
    var propVar = 1

    fun testPublicPropertyInClass() {
        propVal
        propVar
        propVar = 2
    }
}

//FieldWatchpoint! (topPropVal)
val topPropVal = 1

//FieldWatchpoint! (topPropVar)
var topPropVar = 1

fun testPublicTopLevelProperty() {
    topPropVal
    topPropVar
    topPropVar = 2
}

class B(
        //FieldWatchpoint! (bPropVal)
        val bPropVal: Int,
        //FieldWatchpoint! (bPropVar)
        var bPropVar: Int
) {
    fun testConstructorProperty() {
        bPropVal
        bPropVar
        bPropVar = 2
    }
}

class AWithCompanion {
    companion object {
        //FieldWatchpoint! (compPropVal)
        val compPropVal = 1

        //FieldWatchpoint! (compPropVar)
        var compPropVar = 1

        fun testCompanionProperty() {
            compPropVal
            compPropVar
            compPropVar = 2
        }
    }
}

fun main(args: Array<String>) {
    A().testPublicPropertyInClass()
    testPublicTopLevelProperty()
    B(1, 1).testConstructorProperty()
    AWithCompanion.testCompanionProperty()
}

// STEP_OUT: 36