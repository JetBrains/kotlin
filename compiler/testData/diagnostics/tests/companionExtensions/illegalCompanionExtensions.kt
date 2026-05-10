// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    <!WRONG_MODIFIER_TARGET!>companion<!> fun String.foo() {}
    <!WRONG_MODIFIER_TARGET!>companion<!> val String.bar = 1

    <!WRONG_MODIFIER_TARGET!>companion<!> fun noReceiverType() {}
    <!WRONG_MODIFIER_TARGET!>companion<!> val noReceiverTypeProp = 1

    companion object {
        <!WRONG_MODIFIER_TARGET!>companion<!> fun String.foo2() {}
        <!WRONG_MODIFIER_TARGET!>companion<!> val String.bar2 = 1

        <!WRONG_MODIFIER_TARGET!>companion<!> fun noReceiverType2() {}
        <!WRONG_MODIFIER_TARGET!>companion<!> val noReceiverTypeProp2 = 1
    }

    companion {
        <!COMPANION_BLOCK_MEMBER_EXTENSION!><!WRONG_MODIFIER_TARGET!>companion<!> fun String.foo3() {}<!>
        <!COMPANION_BLOCK_MEMBER_EXTENSION!><!WRONG_MODIFIER_TARGET!>companion<!> val String.bar3 = 1<!>

        <!WRONG_MODIFIER_TARGET!>companion<!> fun noReceiverType3() {}
        <!WRONG_MODIFIER_TARGET!>companion<!> val noReceiverTypeProp3 = 1
    }
}

object O {
    <!WRONG_MODIFIER_TARGET!>companion<!> fun String.foo() {}
    <!WRONG_MODIFIER_TARGET!>companion<!> val String.bar = 1

    <!WRONG_MODIFIER_TARGET!>companion<!> fun noReceiverType() {}
    <!WRONG_MODIFIER_TARGET!>companion<!> val noReceiverTypeProp = 1
}

fun local() {
    <!WRONG_MODIFIER_TARGET!>companion<!> fun String.foo() {}
    <!WRONG_MODIFIER_TARGET!>companion<!> val <!LOCAL_EXTENSION_PROPERTY!>String<!>.bar = 1

    <!WRONG_MODIFIER_TARGET!>companion<!> fun noReceiverType() {}
    <!WRONG_MODIFIER_TARGET!>companion<!> val noReceiverTypeProp = 1
}

<!WRONG_MODIFIER_TARGET!>companion<!> fun noReceiverType() {}
<!WRONG_MODIFIER_TARGET!>companion<!> val noReceiverTypeProp = 1

fun anonymousFunction() {
    val x: String.() -> String = run {
        <!WRONG_MODIFIER_TARGET!>companion<!> fun String.(): String = ""
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, integerLiteral,
localFunction, localProperty, objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver */
