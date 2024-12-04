// MEMBER_NAME_FILTER: component1
package properties

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)
const val constant = 0

data class My<caret>Class(val property: @Anno("parameter type: $constant") List<@Anno("nested parameter type: $constant") List<@Anno("nested nested parameter type: $constant") Int>>)
