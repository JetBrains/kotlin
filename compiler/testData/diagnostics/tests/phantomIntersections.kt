// FIR_IDENTICAL
// ISSUE: KT-63741

interface AreaInstance {
    fun getExtensionArea(): Int
}

interface ComponentManager : AreaInstance {
    override fun getExtensionArea() = 10
}

interface Project : ComponentManager, AreaInstance

class MockProject : ComponentManager, Project
