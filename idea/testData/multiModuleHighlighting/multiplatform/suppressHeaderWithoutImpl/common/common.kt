// See KT-15601

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect interface Event

@Suppress("SOMETHING_WRONG")
expect class <error descr="[NO_ACTUAL_FOR_EXPECT] Expected class 'Wrong' has no actual declaration in module testModule_JVM for JVM">Wrong</error>
