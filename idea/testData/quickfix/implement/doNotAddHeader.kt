// "Implement members" "true"
// WITH_RUNTIME
// ERROR: Expected interface 'InterfaceWithFuns' has no actual in module light_idea_test_case for JVM
expect interface InterfaceWithFuns {
    fun funInInterface()
}

class <caret>ChildOfInterface : InterfaceWithFuns{
}
