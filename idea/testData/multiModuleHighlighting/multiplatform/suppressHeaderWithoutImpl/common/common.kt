// See KT-15601

@Suppress("HEADER_WITHOUT_IMPLEMENTATION")
expect interface Event

@Suppress("SOMETHING_WRONG")
expect class <error descr="[HEADER_WITHOUT_IMPLEMENTATION] 'header' class 'Wrong' has no implementation in module jvm for JVM">Wrong</error>
