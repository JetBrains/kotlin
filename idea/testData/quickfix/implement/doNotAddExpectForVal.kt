// "Implement members" "true"
// ENABLE_MULTIPLATFORM
// ERROR: Expected interface 'InterfaceWithVals' has no actual declaration in module light_idea_test_case for JVM

fun TODO(s: String): Nothing = null!!

expect interface InterfaceWithVals {
    fun funInInterface()

    val importantVal: Int
}

class <caret>ChildOfInterface : InterfaceWithVals{
}
