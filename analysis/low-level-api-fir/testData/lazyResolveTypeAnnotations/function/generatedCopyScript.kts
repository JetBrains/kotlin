// MEMBER_NAME_FILTER: copy
package properties

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val constant = 0

data class MyC<caret>lass(
    val property1: @Anno("parameter1 type: $constant") List<@Anno("nested parameter1 type: $constant") List<@Anno("nested nested parameter1 type: $constant") Int>>,
    val property2: @Anno("parameter2 type: $constant") Collection<@Anno("nested parameter2 type: $constant") Collection<@Anno("nested nested parameter2 type: $constant") String>>,
)
