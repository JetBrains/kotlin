@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

fun List<Collection<String>>.ba<caret>r() = foo

var @Anno("receiver type $prop") List<@Anno("nested receiver type $prop") Collection<@Anno("nested nested receiver type $prop") String>>.foo
  get() = this
  set(value) {

  }