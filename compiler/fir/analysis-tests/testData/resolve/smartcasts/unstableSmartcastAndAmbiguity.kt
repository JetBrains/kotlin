// RUN_PIPELINE_TILL: BACKEND
abstract class XdSwimlaneSettings {
    abstract val settingsLogic: String
}

class XdIssueBasedSwimlaneSettings : XdSwimlaneSettings() {
    override val settingsLogic: String
        get() = "hello"
}

class XdAgile(var swimlaneSettings: XdSwimlaneSettings?)

fun test(x: XdAgile) {
    val y = x.swimlaneSettings as XdIssueBasedSwimlaneSettings
    x.swimlaneSettings!!.settingsLogic
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, classDeclaration, functionDeclaration, getter, localProperty,
nullableType, override, primaryConstructor, propertyDeclaration, smartcast, stringLiteral */
