fun lib(): String {
    return when {
        topLevelValToConstVal != "topLevelValToConstVal.v2" -> "fail 1"
        topLevelConstValToVal != "topLevelConstValToVal.v1" -> "fail 2"
        X.memberValToConstVal != "memberValToConstVal.v2" -> "fail 3"
        X.memberConstValToVal != "memberConstValToVal.v1" -> "fail 4"

        else -> "OK"
    }
}

