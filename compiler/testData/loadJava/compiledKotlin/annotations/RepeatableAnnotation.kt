// ISSUE: KT-83185
package test

@Repeatable
@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val x: Int)

@Ann(1)
@Ann(2)
class Some
