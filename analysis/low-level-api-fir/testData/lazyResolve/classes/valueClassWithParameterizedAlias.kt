// SKIP_WHEN_OUT_OF_CONTENT_ROOT
package pack

typealias MyTypeAlias<T> = List<T>

@JvmInline
value class Valu<caret>eClass<T>(val value: MyTypeAlias<T>)