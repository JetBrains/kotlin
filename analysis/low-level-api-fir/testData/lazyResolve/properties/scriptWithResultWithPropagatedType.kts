// RESOLVE_SCRIPT

package foo

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)
const val constant = 0

fun explicitType(): @Anno("return type: $constant") List<@Anno("nested return type: $constant") Collection<@Anno("nested nested return type: $constant") String>> = 0

explicitType()
