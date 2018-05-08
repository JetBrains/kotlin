actual class <error descr="[ACTUAL_WITHOUT_EXPECT] Actual class 'Your' has no corresponding expected declaration">Your</error> {

}

expect class <error descr="[NO_ACTUAL_FOR_EXPECT] Expected class 'His' has no actual declaration in module testModule_JVM for JVM">His</error> {

}

// NOTE: can declare expect and actual in platform module
expect class Their {

}

actual class Their {

}
