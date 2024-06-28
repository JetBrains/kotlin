package low

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

val r<caret>esolveMe = incorrectProperty

var incorrectProperty
    get() = field
    set(value: @Anno("setter parameter type $prop") List<@Anno("setter nested parameter type $prop") Collection<@Anno("setter nested nested parameter type $prop") String>>) = value
