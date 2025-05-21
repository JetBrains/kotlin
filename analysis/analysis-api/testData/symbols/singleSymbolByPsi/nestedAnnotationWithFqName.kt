// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
package lib

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Sources(vararg val value: Source)


@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Repeatable
annotation class Source(vararg val value: String)

@Sour<caret>ces(lib.Source(""))
open class Simple