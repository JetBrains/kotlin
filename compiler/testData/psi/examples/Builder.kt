// COMPILATION_ERRORS

val foo = object : AntBuilder() {

  @lazy val groovy = library {
    classpath("$libs/groovy-...")
  }

  @lazy val gant = library {
    File("$gantHome/lib").files.each {
      classpath(it)
    }
  }

  @lazy val JPS = module {
    targetLevel = "1.5"
    classpath(antLayout, gant, groovy)
    src("$projectHome/antLayout/src")
  }

}.build()

class AntBuilder {
  abstract class ClassPathEntry {}

  class Module : ClassPathEntry {
    fun classpath(entries : ClassPathEntry/*...*/) { /*...*/ }
    var targetLevel : String
    fun src(src : String) { /*...*/ }
  }

  class Library : ClassPathEntry {
    fun classpath(entries : ClassPathEntry/*...*/) { /*...*/ }
  }

  fun library(initializer :   Library.() -> Library) {
    val lib = Library()
    lib.initializer()
    return lib
  }

  fun classpath(/*...*/)

  fun module(/*...*/)
}