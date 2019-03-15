// RUNTIME_WITH_SCRIPT_RUNTIME

@DslMarker
annotation class MyDsl

project1 {
    <caret>
}


@MyDsl
fun project1(p: Project.() -> Unit): Project {
    Project(p)
}

@MyDsl
fun project2(p: Project.() -> Unit) {
    Project(p)
}

@MyDsl
class Project(init: Project.() -> Unit) {
    @MyDsl
    fun buildType(init: BuildType.() -> Unit): BuildType {
        return BuildType(this, init)
    }
}

@MyDsl
class BuildType(p: Project, init: BuildType.() -> Unit)

// ORDER: buildType, project1, project2