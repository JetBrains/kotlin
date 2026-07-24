// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class IC(val s: String)

private fun privateTopLevel(ic: IC): IC = ic

var privateTopLevelSetter: IC = IC("")
    private set(value) {

    }

class Public private constructor(val ic: IC) {
    private fun privateMember(ic: IC): IC = ic

    private var privateProperty: IC = IC("")
}

private class PrivateClass(val ic: IC) {
    fun member(ic: IC): IC = ic

    var property: IC = IC("")

    class Nested {
        fun nestedMember(ic: IC): IC = ic
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: PrivateClass.class[member;nestedMember], PrivateKt.class[privateTopLevel], Public.class[privateMember]
// LIGHT_ELEMENTS_NO_DECLARATION: IC.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], PrivateClass.class[getIc-fhib4bs;getProperty-fhib4bs;member-Eh1mVAw;nestedMember-Eh1mVAw;setProperty-K5cTq2M], PrivateKt.class[privateTopLevel-K5cTq2M;setPrivateTopLevelSetter-K5cTq2M], Public.class[getIc-fhib4bs;privateMember-Eh1mVAw]
