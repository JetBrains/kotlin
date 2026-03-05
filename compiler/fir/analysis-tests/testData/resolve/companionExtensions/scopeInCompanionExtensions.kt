// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
class C {
    fun x() {}
    val y = 1

    companion object {
        fun companionFoo() {}
        val companionBar = 1
    }

    companion {
        fun companionBlockFoo() {}
        val companionBlockBar = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>
    }
}

fun C.extensionFun() {}
val C.extensionProp get() = 1

<!WRONG_MODIFIER_TARGET!>companion<!> fun String.irrelvantCompanionExtension() {}
<!WRONG_MODIFIER_TARGET!>companion<!> val String.irrelvantCompanionExtensionProp = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>

<!WRONG_MODIFIER_TARGET!>companion<!> fun C.companionExtFun() {
    <!UNRESOLVED_REFERENCE!>x<!>()
    <!UNRESOLVED_REFERENCE!>y<!>

    <!UNRESOLVED_REFERENCE!>extensionFun<!>()
    <!UNRESOLVED_REFERENCE!>extensionProp<!>

    <!UNRESOLVED_REFERENCE!>companionFoo<!>()
    <!UNRESOLVED_REFERENCE!>companionBar<!>

    companionBlockFoo()
    companionBlockBar

    companionExtFun()
    companionExtProp

    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>irrelvantCompanionExtension<!>()
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>irrelvantCompanionExtensionProp<!>
}

<!WRONG_MODIFIER_TARGET!>companion<!> var C.companionExtProp: Int
    get() {
        <!UNRESOLVED_REFERENCE!>x<!>()
        <!UNRESOLVED_REFERENCE!>y<!>

        <!UNRESOLVED_REFERENCE!>extensionFun<!>()
        <!UNRESOLVED_REFERENCE!>extensionProp<!>

        <!UNRESOLVED_REFERENCE!>companionFoo<!>()
        <!UNRESOLVED_REFERENCE!>companionBar<!>

        companionBlockFoo()
        companionBlockBar

        companionExtFun()
        companionExtProp

        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>irrelvantCompanionExtension<!>()
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>irrelvantCompanionExtensionProp<!>

        return 1
    }
    set(value) {
        <!UNRESOLVED_REFERENCE!>x<!>()
        <!UNRESOLVED_REFERENCE!>y<!>

        <!UNRESOLVED_REFERENCE!>extensionFun<!>()
        <!UNRESOLVED_REFERENCE!>extensionProp<!>

        <!UNRESOLVED_REFERENCE!>companionFoo<!>()
        <!UNRESOLVED_REFERENCE!>companionBar<!>

        companionBlockFoo()
        companionBlockBar

        companionExtFun()
        companionExtProp

        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>irrelvantCompanionExtension<!>()
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>irrelvantCompanionExtensionProp<!>
    }

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, getter,
integerLiteral, objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, setter */
