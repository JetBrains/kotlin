// RUN_PIPELINE_TILL: BACKEND
class Bar(name: () -> String) {
    val name = name()
}
