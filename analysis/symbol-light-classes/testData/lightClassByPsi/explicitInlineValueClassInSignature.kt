inline value class Some(val value: String) {
    fun member(other: Some): Some = other
}

class RegularClass {
    var classProp: Some = Some("1")
    var Some.classPropInExtension: Int
        get() = 1
        set(value) {}

    fun classFunInReturn(): Some = Some("1")
    fun classFunInParameter(s: Some) {}
    fun Some.classFunInExtension() {}
}

interface RegularInterface {
    var interfaceProp: Some

    fun interfaceFunInParameter(s: Some)
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: RegularClass.class[classFunInExtension;classFunInParameter;classFunInReturn;classPropInExtension], RegularInterface.class[interfaceFunInParameter;interfaceProp], Some.class[member]
// LIGHT_ELEMENTS_NO_DECLARATION: RegularClass.class[classFunInExtension-5lyY9Q4;classFunInParameter-5lyY9Q4;classFunInReturn-YO-7n-0;getClassProp-YO-7n-0;getClassPropInExtension-5lyY9Q4;setClassProp-5lyY9Q4;setClassPropInExtension-54afNMI], RegularInterface.class[getInterfaceProp-YO-7n-0;interfaceFunInParameter-5lyY9Q4;setInterfaceProp-5lyY9Q4], Some.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;member-n0s7kec;toString-impl]
