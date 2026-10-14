// LANGUAGE: +CompanionBlocks +CompanionExtensions

var propertyInitLog = ""
var functionInitLog = ""

class TriggeredByObjectProperty {
    companion {
        val blockProp1: String = run { propertyInitLog += "B1 "; "block1" }
    }

    companion object {
        val objectProp: String = run { propertyInitLog += "O "; "object" }
    }

    companion {
        val blockProp2: String = run { propertyInitLog += "B2 "; "block2" }
    }
}

class TriggeredByObjectFunction {
    companion {
        val blockProp = run {
            functionInitLog += "B"
            "block"
        }
    }

    companion object {
        fun touch() = "OK"
    }
}

fun box(): String {
    // Access only via companion object - this should trigger
    // initialization of companion block properties as well,
    // since they belong to the same class's static state.
    val op = TriggeredByObjectProperty.objectProp

    if (op != "object") return "FAIL: objectProp=$op"

    // Companion block properties must have been initialized
    // even though we only accessed the companion object.
    if (TriggeredByObjectProperty.blockProp1 != "block1") return "FAIL: blockProp1=${TriggeredByObjectProperty.blockProp1}"
    if (TriggeredByObjectProperty.blockProp2 != "block2") return "FAIL: blockProp2=${TriggeredByObjectProperty.blockProp2}"

    // All initializers ran in program order.
    if (propertyInitLog != "B1 O B2 ") return "FAIL: property order: $propertyInitLog"

    // Access via companion object function should also trigger
    // initialization of companion block properties exactly once.
    if (functionInitLog != "") return "FAIL: function initial log: $functionInitLog"
    if (TriggeredByObjectFunction.touch() != "OK") return "FAIL: touch"
    if (functionInitLog != "B") return "FAIL: function order: $functionInitLog"
    if (TriggeredByObjectFunction.blockProp != "block") return "FAIL: blockProp=${TriggeredByObjectFunction.blockProp}"
    if (TriggeredByObjectFunction.touch() != "OK") return "FAIL: second touch"
    if (functionInitLog != "B") return "FAIL: function double init: $functionInitLog"

    return "OK"
}
