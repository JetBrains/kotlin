// LIBRARY_PLATFORMS: JVM

@JvmInline
value class Some(val value: String)

var topLevelProp: Some = Some("1")
var Some.topLevelPropInExtension: Int
    get() = 1
    set(value) {}

fun topLevelFunInReturn(): Some = Some("1")
fun topLevelFunInParameter(s: Some) {}
fun Some.topLevelFunInExtension() {}

@JvmOverloads
fun withJvmOverloads(regularParameter: Int = 0, valueClassParameter: Some = Some("str")) {

}

@JvmOverloads
fun withJvmOverloadsButWithoutDefault(valueClassParameter: Some, regularParameter: Int = 0) {

}

@JvmOverloads
fun withJvmOverloadsInDifferentPositions(first: Int = 0, second: Some = Some("1"), third: Int = 2, fourth: Some = Some("3")) {

}

@JvmOverloads
@JvmName("specialName")
fun withJvmOverloadsAndJvmName(first: Int = 0, second: Some = Some("1"), third: Int = 2, fourth: Some = Some("3")) {

}

@JvmOverloads
fun Some.withJvmOverloadsAndValueReceiver(regularParameter: Int = 0, valueClassParameter: Some = Some("str")) {

}

class SomeClass {
    var memberProp: Some = Some("1")
    var Some.memberPropInExtension: Int
        get() = 1
        set(value) {}

    fun memberFunInReturn(): Some = Some("1")
    fun memberFunInParameter(s: Some) {}
    fun Some.memberFunInExtension() {}
}

interface SomeInterface {
    var memberProp: Some
    var Some.memberPropInExtension: Int
        get() = 1
        set(value) {}

    fun memberFunInReturn(): Some
    fun memberFunInParameter(s: Some)
    fun Some.memberFunInExtension()
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: SomeClass.class[memberFunInExtension;memberFunInParameter;memberFunInReturn;memberPropInExtension], SomeInterface.class[memberFunInExtension;memberFunInParameter;memberFunInReturn;memberProp;memberPropInExtension], ValueClassInSignatureKt.class[topLevelFunInExtension;topLevelFunInParameter;topLevelPropInExtension;withJvmOverloadsAndValueReceiver;withJvmOverloadsButWithoutDefault]
// LIGHT_ELEMENTS_NO_DECLARATION: Some.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], SomeClass.class[getMemberProp-YO-7n-0;getMemberPropInExtension-5lyY9Q4;memberFunInExtension-5lyY9Q4;memberFunInParameter-5lyY9Q4;memberFunInReturn-YO-7n-0;setMemberProp-5lyY9Q4;setMemberPropInExtension-54afNMI], SomeInterface.class[getMemberProp-YO-7n-0;getMemberPropInExtension-5lyY9Q4;memberFunInExtension-5lyY9Q4;memberFunInParameter-5lyY9Q4;memberFunInReturn-YO-7n-0;setMemberProp-5lyY9Q4;setMemberPropInExtension-54afNMI], ValueClassInSignatureKt.class[getTopLevelPropInExtension-5lyY9Q4;setTopLevelProp-5lyY9Q4;setTopLevelPropInExtension-54afNMI;topLevelFunInExtension-5lyY9Q4;topLevelFunInParameter-5lyY9Q4;withJvmOverloads-idg56rU;withJvmOverloadsAndValueReceiver-54afNMI;withJvmOverloadsAndValueReceiver-5lyY9Q4;withJvmOverloadsAndValueReceiver-s1Cr6JE;withJvmOverloadsButWithoutDefault-54afNMI;withJvmOverloadsButWithoutDefault-5lyY9Q4;withJvmOverloadsInDifferentPositions-8--ZunY;withJvmOverloadsInDifferentPositions-idg56rU;withJvmOverloadsInDifferentPositions-nc4VKrw]