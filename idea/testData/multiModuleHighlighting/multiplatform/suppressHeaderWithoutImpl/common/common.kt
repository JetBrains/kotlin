// See KT-15601

@Suppress("HEADER_WITHOUT_IMPLEMENTATION")
header interface Event

@Suppress("SOMETHING_WRONG")
header class <error descr="[HEADER_WITHOUT_IMPLEMENTATION] Header declaration 'Wrong' has no implementation in module jvm for JVM">Wrong</error>