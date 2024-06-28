package one

annotation class Anno(val s: String)

@Anno(fun(): String {

}())
class TopLevelClass