package low

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

var reso<caret>lveMe
    get() = field
    set(value: @Anno("setter parameter type $prop") List<@Anno("setter nested parameter type $prop") Collection<@Anno("setter nested nested parameter type $prop") String>>) = value
