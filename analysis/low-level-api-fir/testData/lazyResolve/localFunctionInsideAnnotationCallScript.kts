package one

annotation class Anno(val s: String)

@Anno(f<caret>un(): String {

}())
class TopLevelClass