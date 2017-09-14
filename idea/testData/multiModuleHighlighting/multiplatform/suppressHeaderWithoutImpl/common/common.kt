// See KT-15601

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect interface Event

@Suppress("SOMETHING_WRONG")
expect class <error descr="[NO_ACTUAL_FOR_EXPECT] 'header' class 'Wrong' has no implementation in module jvm for JVM">Wrong</error>
