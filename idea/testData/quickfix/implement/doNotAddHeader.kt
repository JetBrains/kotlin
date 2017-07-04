// "Implement members" "true"
// WITH_RUNTIME
// ERROR: Header declaration 'InterfaceWithFuns' has no implementation in module light_idea_test_case for JVM
header interface InterfaceWithFuns {
    fun funInInterface()
}

class <caret>ChildOfInterface : InterfaceWithFuns{
}